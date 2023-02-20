package com.xxuz.piclane.smartpower

import com.xxuz.piclane.smartpower.sk.*
import com.xxuz.piclane.smartpower.sk.command.SendToSecurity
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

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
        private val logger = LoggerFactory.getLogger(PowerObserver::class.java)
    }

    private lateinit var sk: SkStack

    private lateinit var ipAddr: IpAddr

    private lateinit var thread: Thread

    @Volatile
    private var stopped = false

    @PostConstruct
    private fun init() {
        SkStack.addShutdownHook(Thread {
            stopped = true
        })

        sk = SkStack(devicePath)

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

        thread = Thread {
            try {
                mainLoop()
            } catch (e: Throwable) {
                if(stopped) {
                    return@Thread
                }
                logger.error("メインループでエラーが発生しました", e)
            }
        }
        thread.name = "PowerObserver"
        thread.start()
    }

    @PreDestroy
    private fun destroy() {
        stopped = true
        thread.interrupt()
        thread.join()

        sk.close()
    }

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
        ).toByteBuffer()

        while(!Thread.interrupted() && !stopped) {
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
