package CNPROJECT.SERVER;

import com.google.gson.*;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.security.spec.*;
import java.util.Base64;
import java.util.Random;

/**
 * Server class that listens for secure connections, performs complex number
 * calculations,
 * and returns encrypted responses.
 */
public class Servertcp {

    private static final int PORT = 11243;

    public static void main(String[] args) throws Exception {
        // Ensure public/private keys exist; generate using Keygen if missing
        File pub = new File("public.key");
        File priv = new File("private.key");

        if (!pub.exists() || !priv.exists()) {
            System.out.println("Keys not found. Generating using Keygen...");
            Keygen.generateKeysIfMissing(); // Call static method from Keygen.java
        }

        // Load the server's private key
        PrivateKey privateKey = loadPrivateKey("private.key");
        System.out.println("Loaded private key from private.key");

        // Start the server socket
        try (ServerSocket ss = new ServerSocket(PORT)) {
            System.out.println("Server is up and listening on port " + PORT);

            while (true) {
                try (Socket s = ss.accept()) {
                    System.out.println("Accepted a connection from " + s.getRemoteSocketAddress());
                    handleConnection(s, privateKey);
                } catch (Exception ex) {
                    System.err.println("Error handling client: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * Handles communication with a connected client.
     */
    private static void handleConnection(Socket s, PrivateKey privateKey) throws Exception {
        InputStream in = s.getInputStream();
        String requestJson = new String(in.readAllBytes(), StandardCharsets.UTF_8);

        if (requestJson.isEmpty()) {
            System.out.println("Received empty request. Closing connection.");
            return;
        }

        Gson gson = new Gson();
        JsonObject packet = gson.fromJson(requestJson, JsonObject.class);

        byte[] encKey = Base64.getDecoder().decode(packet.get("enc_key").getAsString());
        byte[] nonce = Base64.getDecoder().decode(packet.get("nonce").getAsString());
        byte[] ct = Base64.getDecoder().decode(packet.get("ct").getAsString());

        // RSA-OAEP decrypt AES session key
        Cipher rsa = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        rsa.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] aesKey = rsa.doFinal(encKey);

        // AES-GCM decrypt payload
        Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(128, nonce);
        SecretKey secretKey = new javax.crypto.spec.SecretKeySpec(aesKey, "AES");
        aes.init(Cipher.DECRYPT_MODE, secretKey, spec);
        byte[] plaintext = aes.doFinal(ct);

        JsonObject req = gson.fromJson(new String(plaintext, StandardCharsets.UTF_8), JsonObject.class);
        JsonObject result = computeOperation(req);

        // Encrypt response
        JsonObject respObj = new JsonObject();
        respObj.add("result", result);
        byte[] respBytes = gson.toJson(respObj).getBytes(StandardCharsets.UTF_8);

        byte[] nonceResp = new byte[12];
        new SecureRandom().nextBytes(nonceResp);
        Cipher aesEnc = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec2 = new GCMParameterSpec(128, nonceResp);
        aesEnc.init(Cipher.ENCRYPT_MODE, secretKey, spec2);
        byte[] ctResp = aesEnc.doFinal(respBytes);

        JsonObject outPacket = new JsonObject();
        outPacket.addProperty("nonce", Base64.getEncoder().encodeToString(nonceResp));
        outPacket.addProperty("ct", Base64.getEncoder().encodeToString(ctResp));

        OutputStream os = s.getOutputStream();
        os.write(gson.toJson(outPacket).getBytes(StandardCharsets.UTF_8));
        os.flush();
        System.out.println("Encrypted response sent to client.");
    }

    /**
     * Computes the requested complex number operation using the external Complex
     * class.
     */
    private static JsonObject computeOperation(JsonObject req) {
        String op = req.get("op").getAsString();
        Complex result;

        switch (op.toLowerCase()) {
            case "add": {
                JsonObject z1 = req.getAsJsonObject("z1");
                JsonObject z2 = req.getAsJsonObject("z2");
                Complex c1 = new Complex(z1.get("re").getAsDouble(), z1.get("im").getAsDouble());
                Complex c2 = new Complex(z2.get("re").getAsDouble(), z2.get("im").getAsDouble());
                result = c1.add(c2);
                break;
            }
            case "sub": {
                JsonObject z1 = req.getAsJsonObject("z1");
                JsonObject z2 = req.getAsJsonObject("z2");
                Complex c1 = new Complex(z1.get("re").getAsDouble(), z1.get("im").getAsDouble());
                Complex c2 = new Complex(z2.get("re").getAsDouble(), z2.get("im").getAsDouble());
                result = c1.sub(c2);
                break;
            }
            case "mul": {
                JsonObject z1 = req.getAsJsonObject("z1");
                JsonObject z2 = req.getAsJsonObject("z2");
                Complex c1 = new Complex(z1.get("re").getAsDouble(), z1.get("im").getAsDouble());
                Complex c2 = new Complex(z2.get("re").getAsDouble(), z2.get("im").getAsDouble());
                result = c1.mul(c2);
                break;
            }
            case "div": {
                JsonObject z1 = req.getAsJsonObject("z1");
                JsonObject z2 = req.getAsJsonObject("z2");
                Complex c1 = new Complex(z1.get("re").getAsDouble(), z1.get("im").getAsDouble());
                Complex c2 = new Complex(z2.get("re").getAsDouble(), z2.get("im").getAsDouble());
                result = c1.div(c2);
                break;
            }
            case "exp": {
                JsonObject z = req.getAsJsonObject("z");
                Complex cz = new Complex(z.get("re").getAsDouble(), z.get("im").getAsDouble());
                result = cz.exp();
                break;
            }
            case "log": {
                JsonObject z = req.getAsJsonObject("z");
                Complex cz = new Complex(z.get("re").getAsDouble(), z.get("im").getAsDouble());
                result = cz.log();
                break;
            }
            case "pow": {
                JsonObject z1 = req.getAsJsonObject("z1");
                JsonObject z2 = req.getAsJsonObject("z2");
                Complex c1 = new Complex(z1.get("re").getAsDouble(), z1.get("im").getAsDouble());
                Complex c2 = new Complex(z2.get("re").getAsDouble(), z2.get("im").getAsDouble());
                result = c1.pow(c2);
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported operation: " + op);
        }

        JsonObject r = new JsonObject();
        r.addProperty("re", result.re);
        r.addProperty("im", result.im);
        return r;
    }

    /**
     * Loads the server's private key from a DER file.
     */
    private static PrivateKey loadPrivateKey(String path) throws Exception {
        byte[] keyBytes = Files.readAllBytes(Paths.get(path));
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }
}