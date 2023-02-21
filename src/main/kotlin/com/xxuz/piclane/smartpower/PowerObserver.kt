package com.xxuz.piclane.smartpower

import com.xxuz.piclane.smartpower.sk.*
import com.xxuz.piclane.smartpower.sk.command.SendToSecurity
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
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
    }

    /** 通信スレッド */
    private lateinit var thread: Thread

    /** ステータス */
    private var status: AtomicReference<Status> = AtomicReference(Status.Ready)

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
            logger.info("SKVER: ${sk.version()}")
            sk.setPassword(devicePassword)
            sk.setRouteBId(deviceRbid)

            val pans = sk.activeScan(0xffffffff, 6)
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
            val frame = EchoNetLiteFrame(
                ehd1 = 0x10, // ECHONET Lite規格であることを示す (02-3.2.1.1)
                ehd2 = 0x81, // 電文形式 1(規定電文形式)であることを示す (02-3.2.1.2)
                tid = 1,
                edata = EchoNetLiteFrame.EDATA(
                    sEoj = eojSrc,
                    dEoj = eojDst,
                    esv = 0x62, // プロパティ値読み出し要求
                    op = listOf(
                        EchoNetLiteFrame.OP(
                            epc = 0xE7, // 瞬時電力計測値
                        ),
                        EchoNetLiteFrame.OP(
                            epc = 0xE8 // 瞬時電流計測値
                        ),
                    )
                )
            ).toByteArray()

            while(status.get() != Status.Stopping) {
                sk.sendTo(1, ipAddr, 0x0E1A, SendToSecurity.ENCRYPT, frame)
                val rx = try {
                    sk.waitForRxUdp(10000)
                } catch (e: SkTimeoutException) {
                    continue
                } catch (e: SkEofException) {
                    break
                }

                val rxFrame = EchoNetLiteFrame.fromHex(rx.data)
                val rxEData = rxFrame.edata
                if(rxEData.sEoj == eojDst && rxEData.esv == 0x72 /** プロパティ値読み出し応答 */) {
                    rxEData.op.forEach { op ->
                        val buf = op.toByteBuffer()
                        when(op.epc) {
                            0xE7 -> {
                                println("瞬時電力計測値 ${buf.int}W")
                            }
                            0xE8 -> {
                                val rPhase = buf.short.toDouble() * 0.1
                                val tPhase = buf.short.toDouble() * 0.1
                                println("瞬時電流計測値 R相 ${rPhase}A, T相 ${tPhase}A")
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * ステータス
     */
    private enum class Status {
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
