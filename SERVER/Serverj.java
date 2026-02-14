package CNPROJECT.SERVER;

import com.google.gson.*;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.security.spec.*;
import java.util.Base64;

/**
 * Secure server that performs complex number operations.
 */
public class Serverj {
    private static final int PORT = 11243;

    public static void main(String[] args) throws Exception {
        Keygenj.generateKeysIfMissing();

        PrivateKey priv = loadPrivateKey("private.key");
        System.out.println("Private key loaded.");
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Server started on port " + PORT);

        Gson gson = new Gson();

        while (true) {
            Socket s = serverSocket.accept();
            System.out.println("Client connected: " + s.getInetAddress());

            try (InputStream in = s.getInputStream();
                    OutputStream out = s.getOutputStream()) {

                // Receive encrypted packet
                String packetStr = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                JsonObject packet = gson.fromJson(packetStr, JsonObject.class);

                // Extract fields
                byte[] encKey = Base64.getDecoder().decode(packet.get("enc_key").getAsString());
                byte[] nonce = Base64.getDecoder().decode(packet.get("nonce").getAsString());
                byte[] ct = Base64.getDecoder().decode(packet.get("ct").getAsString());

                // Decrypt AES key with RSA
                Cipher rsa = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
                rsa.init(Cipher.DECRYPT_MODE, priv);
                byte[] aesKey = rsa.doFinal(encKey);

                // Decrypt ciphertext with AES-GCM
                SecretKey secretKey = new javax.crypto.spec.SecretKeySpec(aesKey, "AES");
                Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
                GCMParameterSpec spec = new GCMParameterSpec(128, nonce);
                aes.init(Cipher.DECRYPT_MODE, secretKey, spec);
                byte[] pt = aes.doFinal(ct);
                String payload = new String(pt, StandardCharsets.UTF_8);
                System.out.println("Decrypted client JSON: " + payload);

                // Parse request JSON
                JsonObject req = gson.fromJson(payload, JsonObject.class);
                String op = req.get("op").getAsString();
                JsonObject resp = new JsonObject();

                try {
                    Complex result = handleOperation(op, req);
                    resp.addProperty("result", result.toString());
                } catch (Exception e) {
                    resp.addProperty("error", "Invalid operation: " + e.getMessage());
                }

                String respPlain = gson.toJson(resp);
                System.out.println("Computed result JSON (before encryption): " + respPlain);

                // Encrypt response using AES-GCM with a new nonce
                byte[] nonceResp = new byte[12];
                new SecureRandom().nextBytes(nonceResp);
                Cipher aesEnc = Cipher.getInstance("AES/GCM/NoPadding");
                GCMParameterSpec specEnc = new GCMParameterSpec(128, nonceResp);
                aesEnc.init(Cipher.ENCRYPT_MODE, secretKey, specEnc);
                byte[] ctResp = aesEnc.doFinal(respPlain.getBytes(StandardCharsets.UTF_8));

                JsonObject respPacket = new JsonObject();
                respPacket.addProperty("nonce", Base64.getEncoder().encodeToString(nonceResp));
                respPacket.addProperty("ct", Base64.getEncoder().encodeToString(ctResp));

                String finalResp = gson.toJson(respPacket);

                out.write(finalResp.getBytes(StandardCharsets.UTF_8));
                out.flush();
                s.shutdownOutput();
                System.out.println("Response sent to client.\n");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static Complex handleOperation(String op, JsonObject req) {
        switch (op) {
            case "add":
            case "sub":
            case "mul":
            case "div": {
                JsonObject z1 = req.get("z1").getAsJsonObject();
                JsonObject z2 = req.get("z2").getAsJsonObject();
                Complex c1 = new Complex(z1.get("re").getAsDouble(), z1.get("im").getAsDouble());
                Complex c2 = new Complex(z2.get("re").getAsDouble(), z2.get("im").getAsDouble());
                return switch (op) {
                    case "add" -> c1.add(c2);
                    case "sub" -> c1.sub(c2);
                    case "mul" -> c1.mul(c2);
                    case "div" -> c1.div(c2);
                    default -> throw new IllegalArgumentException("Unsupported operation");
                };
            }
            case "exp":
            case "log": {
                JsonObject z = req.get("z").getAsJsonObject();
                Complex c = new Complex(z.get("re").getAsDouble(), z.get("im").getAsDouble());
                return op.equals("exp") ? c.exp() : c.log();
            }
            case "pow": {
                JsonObject z1 = req.get("z1").getAsJsonObject();
                JsonObject z2 = req.get("z2").getAsJsonObject();
                Complex base = new Complex(z1.get("re").getAsDouble(), z1.get("im").getAsDouble());
                Complex exp = new Complex(z2.get("re").getAsDouble(), z2.get("im").getAsDouble());
                return base.pow(exp);
            }
            default:
                throw new IllegalArgumentException("Unknown operation: " + op);
        }
    }

    private static PrivateKey loadPrivateKey(String path) throws Exception {
        byte[] keyBytes = Files.readAllBytes(Paths.get(path));
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }
}