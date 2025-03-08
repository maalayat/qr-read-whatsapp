package org.example

import com.google.zxing.*
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.common.HybridBinarizer
import it.auties.whatsapp.api.QrHandler
import it.auties.whatsapp.api.Whatsapp
import it.auties.whatsapp.model.info.MessageInfo
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

fun main() {
    Whatsapp.webBuilder()
        .lastConnection()
        .unregistered(QrHandler.toTerminal())
        .addLoggedInListener { api -> println("Connected to Whatsapp: ${api.store().privacySettings()}") }
        .addDisconnectedListener { reason -> println("Disconnected from Whatsapp: $reason") }
        .addNewMessageListener { api, messageInfo -> processMessage(api, messageInfo) }
        .connect()
        .join()
        .awaitDisconnection()
}

fun processMessage(api: Whatsapp?, messageInfo: MessageInfo<*>?) {
    println("Receiving a new message")
    messageInfo?.message()?.imageMessage?.ifPresent { imageMessage ->
        api?.downloadMedia(imageMessage)?.join()?.ifPresent { bytearray ->
            val decodedQrText = decodeQrFromMedia(bytearray)
            api.sendMessage(messageInfo.senderJid(), decodedQrText)
        }
    }
}

fun decodeQrFromMedia(mediaData: ByteArray): String {
    return try {
        println("Convert mediaData to BufferedImage")
        val inputStream = ByteArrayInputStream(mediaData)
        val bufferedImage: BufferedImage? = ImageIO.read(inputStream)

        println("Prepare the image for ZXing")
        val luminanceSource = BufferedImageLuminanceSource(bufferedImage)
        val binaryBitmap = BinaryBitmap(HybridBinarizer(luminanceSource))

        println("Configure the ZXing QR code reader")
        val reader = MultiFormatReader()
        val hints = mapOf(
            DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
            DecodeHintType.TRY_HARDER to true
        )

        println("Attempt decoding the QR code")
        val result = reader.decode(binaryBitmap, hints)
        val qrText = result.text
        "Decoded QR text: $qrText"
    } catch (e: NotFoundException) {
        println("Error decoding QR code: ${e.message}")
        "Error processing the image. Please try again."
    }
}
