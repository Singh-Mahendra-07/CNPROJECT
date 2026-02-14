
package CNPROJECT.CLIENT;

import com.google.gson.*;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.security.spec.*;
import java.util.*;

/**
 * Client class for secure communication with the Server.
 */
public class Clienttcp {
    private static final String HOST = "127.0.0.1"; // Server address
    private static final int PORT = 11243; // Must match Server.java's PORT

    public static void main(String[] args) throws Exception {
        // Load public key
        PublicKey pub = loadPublicKey("public.key");
        System.out.println("Loaded server's public key from public.key");

        Gson gson = new Gson();
        Scanner sc = new Scanner(System.in);

        System.out.println("Choose operation (add / sub / mul / div / exp / log / pow): ");
        String op = sc.nextLine().trim().toLowerCase();

        JsonObject req = new JsonObject();
        req.addProperty("op", op);

        switch (op) {
            case "add":
            case "sub":
            case "mul":
            case "div": {
                // For these, two complex numbers are needed
                System.out.println("Enter real part of first number (z1.re): ");
                double re1 = sc.nextDouble();
                System.out.println("Enter imaginary part of first number (z1.im): ");
                double im1 = sc.nextDouble();

                System.out.println("Enter real part of second number (z2.re): ");
                double re2 = sc.nextDouble();
                System.out.println("Enter imaginary part of second number (z2.im): ");
                double im2 = sc.nextDouble();

                JsonObject z1 = new JsonObject();
                z1.addProperty("re", re1);
                z1.addProperty("im", im1);
                JsonObject z2 = new JsonObject();
                z2.addProperty("re", re2);
                z2.addProperty("im", im2);
                req.add("z1", z1);
                req.add("z2", z2);
                break;
            }
            case "exp":
            case "log": {
                System.out.println("Enter real part: ");
                double re = sc.nextDouble();
                System.out.println("Enter imaginary part: ");
                double im = sc.nextDouble();

                JsonObject z = new JsonObject();
                z.addProperty("re", re);
                z.addProperty("im", im);
                req.add("z", z);
                break;
            }
            case "pow": {
                System.out.println("Enter real part of base (z1.re): ");
                double re1 = sc.nextDouble();
                System.out.println("Enter imaginary part of base (z1.im): ");
                double im1 = sc.nextDouble();

                System.out.println("Enter real part of exponent (z2.re): ");
                double re2 = sc.nextDouble();
                System.out.println("Enter imaginary part of exponent (z2.im): ");
                double im2 = sc.nextDouble();

                JsonObject z1 = new JsonObject();
                z1.addProperty("re", re1);
                z1.addProperty("im", im1);
                JsonObject z2 = new JsonObject();
                z2.addProperty("re", re2);
                z2.addProperty("im", im2);
                req.add("z1", z1);
                req.add("z2", z2);
                break;
            }
            default:
                System.out.println("Invalid operation! Use add, sub, mul, div, exp, log, or pow.");
                sc.close();
                return;
        }

        sc.close();
        System.out.println("Request JSON (before encryption): " + gson.toJson(req));

        // ========== AES Key and Nonce Generation ==========
        byte[] aesKey = new byte[32];
        new SecureRandom().nextBytes(aesKey);
        byte[] nonce = new byte[12];
        new SecureRandom().nextBytes(nonce);

        SecretKey secretKey = new javax.crypto.spec.SecretKeySpec(aesKey, "AES");
        Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(128, nonce);
        aes.init(Cipher.ENCRYPT_MODE, secretKey, spec);
        byte[] plaintext = gson.toJson(req).getBytes(StandardCharsets.UTF_8);
        byte[] ct = aes.doFinal(plaintext);

        // ========== Encrypt AES key using RSA ==========
        Cipher rsa = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        rsa.init(Cipher.ENCRYPT_MODE, pub);
        byte[] encKey = rsa.doFinal(aesKey);

        JsonObject packet = new JsonObject();
        packet.addProperty("enc_key", Base64.getEncoder().encodeToString(encKey));
        packet.addProperty("nonce", Base64.getEncoder().encodeToString(nonce));
        packet.addProperty("ct", Base64.getEncoder().encodeToString(ct));
        String packetStr = gson.toJson(packet);

        // ========== Send Request & Receive Response ==========
        try (Socket s = new Socket(HOST, PORT)) {
            OutputStream os = s.getOutputStream();
            os.write(packetStr.getBytes(StandardCharsets.UTF_8));
            os.flush();
            s.shutdownOutput();

            InputStream in = s.getInputStream();
            String respStr = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            JsonObject respPacket = gson.fromJson(respStr, JsonObject.class);

            byte[] nonceResp = Base64.getDecoder().decode(respPacket.get("nonce").getAsString());
            byte[] ctResp = Base64.getDecoder().decode(respPacket.get("ct").getAsString());

            Cipher aesDec = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec2 = new GCMParameterSpec(128, nonceResp);
            aesDec.init(Cipher.DECRYPT_MODE, secretKey, spec2);
            byte[] ptResp = aesDec.doFinal(ctResp);
            String payload = new String(ptResp, StandardCharsets.UTF_8);

            System.out.println("Server response JSON: " + payload);
        }
    }

    // Load RSA Public Key from file
    private static PublicKey loadPublicKey(String path) throws Exception {
        byte[] keyBytes = Files.readAllBytes(Paths.get(path));
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }
}
