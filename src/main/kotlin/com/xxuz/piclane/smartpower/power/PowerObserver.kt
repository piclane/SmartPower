package com.xxuz.piclane.smartpower.power

import com.xxuz.piclane.smartpower.model.Cumulative
import com.xxuz.piclane.smartpower.model.Current
import com.xxuz.piclane.smartpower.model.Instantaneous
import com.xxuz.piclane.smartpower.sk.*
import com.xxuz.piclane.smartpower.sk.command.SendToSecurity
import com.xxuz.piclane.smartpower.sk.event.PanDesc
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

@Service
class PowerObserver(
    @Value("\${app.device.path}")
    private val devicePath: String,

    @Value("\${app.device.password}")
    private val devicePassword: String,

    @Value("\${app.device.rbid}")
    private val deviceRbid: String,
) {
    companion object {
        /** ロガー */
        private val logger = LoggerFactory.getLogger(PowerObserver::class.java)

        /** プロパティー名: 瞬時電力計測値 (W) */
        const val PROPERTY_INSTANTANEOUS_POWER = "instantaneousPower"

        /** プロパティー名: 瞬時電流計測値 (A) */
        const val PROPERTY_INSTANTANEOUS_CURRENT = "instantaneousCurrent"

        /** プロパティー名: 瞬時計測値 */
        const val PROPERTY_INSTANTANEOUS = "instantaneous"

        /** プロパティー名: 積算計測値 */
        const val PROPERTY_CUMULATIVE = "cumulative"
    }

    /** 通信スレッド */
    private lateinit var thread: Thread

    /** ステータス */
    private val status: AtomicReference<Status> = AtomicReference(Status.Ready)

    /** 瞬時計測値 */
    private val instantaneous = AtomicReference(Instantaneous.ZERO)

    /** 累積計測値 */
    private val cumulative = AtomicReference<Cumulative>()

    /** PropertyChangeListener の配列 */
    private val propertyChangeListeners = CopyOnWriteArrayList<PropertyChangeListener>()

    /** ステータスを取得します */
    fun getStatus(): Status = status.get()

    /** 瞬時計測値を取得します */
    fun getInstantaneous() = instantaneous.get()

    /** 積算計測値を取得します */
    fun getCumulative() = cumulative.get()

    /**
     * PropertyChangeListener を追加します
     */
    fun addPropertyChangeListener(listener: PropertyChangeListener) {
        propertyChangeListeners.add(listener)
    }

    /**
     * PropertyChangeListener を削除します
     */
    fun removePropertyChangeListener(listener: PropertyChangeListener) {
        propertyChangeListeners.remove(listener)
    }

    /**
     * PropertyChangeEvent を発火します
     */
    private fun <T> firePropertyChangeEvent(propertyName: String, oldValue: T, newValue: T) {
        if(oldValue == newValue) {
            return
        }
        val event = PropertyChangeEvent(this, propertyName, oldValue, newValue)
        propertyChangeListeners.forEach {
            try {
                it.propertyChange(event)
            } catch (e: Exception) {
                logger.error("PropertyChangeListener.propertyChange failed.", e)
            }
        }
    }

    /**
     * 通信スレッド開始
     */
    @PostConstruct
    private fun start() {
        thread = CommunicationThread()
        thread.name = "PowerObserver"
        thread.start()
    }

    /**
     * 通信スレッド終了
     */
    @PreDestroy
    private fun destroy() {
        status.compareAndSet(Status.Started, Status.Stopping)
        thread.join()
    }

    private inner class CommunicationThread: Thread() {
        /** SkStack */
        private val sk: SkStack = SkStack(devicePath)

        /** スマートメーターのアドレス */
        private lateinit var ipAddr: IpAddr

        override fun run() {
            try {
                logger.info("初期化を開始します")
                status.set(Status.Starting)

                // 初期化
                try {
                    init()
                } catch(e: Throwable) {
                    logger.error("初期化に失敗しました", e)
                    return
                }

                status.set(Status.Started)

                SkStack.addShutdownHook(Thread {
                    status.compareAndSet(Status.Started, Status.Stopping)
                })

                logger.info("初期化が完了しました")

                // メインループ
                try {
                    mainLoop()
                } catch (e: Throwable) {
                    if(status.get() == Status.Stopping) {
                        return
                    }
                    logger.error("メインループでエラーが発生しました", e)
                }
            } finally {
                // 後始末
                status.set(Status.Stopped)
                sk.close()
            }
        }

        /**
         * 初期化
         */
        private fun init() {
            sk.open()

            logger.info("SKVER: ${sk.version()}")
            sk.setPassword(devicePassword)
            sk.setRouteBId(deviceRbid)

            val pans = mutableListOf<PanDesc>()
            var trial = 0
            do {
                pans.addAll(sk.activeScan(0xffffffff, 4 + trial))
            } while (pans.isEmpty() && ++trial < 4)
            if (pans.isEmpty()) {
                throw RuntimeException("スマートメーターが見つかりませんでした")
            }

            val pan = pans.first()
            sk.setRegister("S2", pan.channelHex)
            sk.setRegister("S3", pan.panIdHex)
            ipAddr = sk.ll64(pan.addr)
            sk.join(ipAddr)
        }

        /**
         * メインループ
         */
        private fun mainLoop() {
            val eojSrc = EchoNetLiteFrame.EOJ(0x05, 0xFF, 0x01) // 送信元：管理・操作関連機器クラスグループ／コントローラクラス (Appendix-3.6.2)
            val eojDst = EchoNetLiteFrame.EOJ(0x02, 0x88, 0x01) // 送信先：住宅・設備関連機器クラスグループ／低圧スマート電力量メータクラス (Appendix-3.3.25)
            val defaultOp = listOf(
                EchoNetLiteFrame.OP(epc = 0xE7), // 瞬時電力計測値
                EchoNetLiteFrame.OP(epc = 0xE8), // 瞬時電流計測値

                EchoNetLiteFrame.OP(epc = 0xE0), // 積算電力量計測値(正方向計測値)
                EchoNetLiteFrame.OP(epc = 0xD3), // 係数
                EchoNetLiteFrame.OP(epc = 0xE1), // 積算電力量単位
                // EchoNetLiteFrame.OP(epc = 0xD7), // 積算電力量有効桁数
            )
            val frame = EchoNetLiteFrame(
                ehd1 = 0x10, // ECHONET Lite規格であることを示す (02-3.2.1.1)
                ehd2 = 0x81, // 電文形式 1(規定電文形式)であることを示す (02-3.2.1.2)
                tid = 1,
                edata = EchoNetLiteFrame.EDATA(
                    sEoj = eojSrc,
                    dEoj = eojDst,
                    esv = 0x62, // プロパティ値読み出し要求
                    op = defaultOp,
                )
            ).toByteArray()

            while(status.get() != Status.Stopping) {
                sk.sendTo(1, ipAddr, 0x0E1A, SendToSecurity.ENCRYPT, frame)
                val rx = try {
                    sk.waitForRxUdp(10000) { it.lPort == 0x0E1A }
                } catch (e: SkTimeoutException) {
                    continue
                } catch (e: SkEofException) {
                    break
                }

                val rxFrame = EchoNetLiteFrame.fromHex(rx.data)
                val rxEData = rxFrame.edata
                if(rxEData.sEoj == eojDst && rxEData.esv == 0x72 /** プロパティ値読み出し応答 */) {
                    val oldInstantaneous = instantaneous.get()
                    val oldCumulative = cumulative.get()
                    var newPower = oldInstantaneous.power
                    var newCurrent = oldInstantaneous.current
                    var rawPositiveEnergy = 0L
                    var coefficient = 1L
                    var energyUnit = 0
                    rxEData.op.forEach { op ->
                        val buf = op.toByteBuffer()
                        when(op.epc) {
                            0xE7 -> {
                                newPower = buf.int
                                firePropertyChangeEvent(PROPERTY_INSTANTANEOUS_POWER, oldInstantaneous.power, newPower)
                            }
                            0xE8 -> {
                                newCurrent = Current(
                                    rPhase = buf.short.toDouble() * 0.1,
                                    tPhase = buf.short.toDouble() * 0.1,
                                )
                                firePropertyChangeEvent(PROPERTY_INSTANTANEOUS_CURRENT, oldInstantaneous.current, newCurrent)
                            }
                            0xE0 -> { // 積算電力量計測値(正方向計測値)
                                val rawValue = buf.int
                                rawPositiveEnergy = rawValue.toLong() and 0xFFFFFFFFL
                            }
                            0xD3 -> { // 係数（積算値に掛ける値）
                                val rawValue = buf.int
                                coefficient = rawValue.toLong() and 0xFFFFFFFFL
                            }
                            0xE1 -> { // 積算電力量単位
                                energyUnit = buf.get().toInt() and 0xFF
                            }
                        }
                    }
                    // 瞬間値
                    val newInstantaneous = Instantaneous(
                        power = newPower,
                        current = newCurrent,
                    )
                    instantaneous.set(newInstantaneous)
                    firePropertyChangeEvent(PROPERTY_INSTANTANEOUS, oldInstantaneous, newInstantaneous)

                    // 積算値
                    val newCumulative = Cumulative(
                        forwardEnergy = calculateActualEnergy(rawPositiveEnergy, coefficient, energyUnit)
                    )
                    cumulative.set(newCumulative)
                    firePropertyChangeEvent(PROPERTY_CUMULATIVE, oldCumulative, newCumulative)
                }
            }
        }
    }

    /**
     * 積算電力量を実際の値に変換する
     * @param rawValue 生の積算電力量値
     * @param coefficient 係数
     * @param energyUnit 単位コード（0x00: 1kWh, 0x01: 0.1kWh, 0x02: 0.01kWh, 0x03: 0.001kWh, 0x04: 0.0001kWh等）
     * @return 実際の積算電力量（kWh単位）
     */
    private fun calculateActualEnergy(rawValue: Long, coefficient: Long, energyUnit: Int): Double {
        // 係数を適用
        val valueWithCoefficient = rawValue * coefficient

        // 単位による変換
        return when (energyUnit) {
            0x00 -> valueWithCoefficient.toDouble() // 1kWh
            0x01 -> valueWithCoefficient.toDouble() * 0.1 // 0.1kWh
            0x02 -> valueWithCoefficient.toDouble() * 0.01 // 0.01kWh
            0x03 -> valueWithCoefficient.toDouble() * 0.001 // 0.001kWh
            0x04 -> valueWithCoefficient.toDouble() * 0.0001 // 0.0001kWh
            0x0A -> valueWithCoefficient.toDouble() * 10 // 10kWh
            0x0B -> valueWithCoefficient.toDouble() * 100 // 100kWh
            0x0C -> valueWithCoefficient.toDouble() * 1000 // 1000kWh
            0x0D -> valueWithCoefficient.toDouble() * 10000 // 10000kWh
            else -> valueWithCoefficient.toDouble() // デフォルトは変換なし
        }
    }

    /**
     * ステータス
     */
    enum class Status {
        /** 初期化前 */
        Ready,
        /** 初期化中 */
        Starting,
        /** 開始 */
        Started,
        /** 停止中 */
        Stopping,
        /** 停止 */
        Stopped,
    }
}
