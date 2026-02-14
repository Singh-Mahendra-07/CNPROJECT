// version 1.0
// package CNPROJECT.SERVER;

// // Server.java
// import com.google.gson.*;
// import javax.crypto.*;
// import javax.crypto.spec.GCMParameterSpec;
// import java.io.*;
// import java.net.ServerSocket;
// import java.net.Socket;
// import java.nio.charset.StandardCharsets;
// import java.nio.file.*;
// import java.security.*;
// import java.security.spec.*;
// import java.util.Base64;
// import java.util.Map;
// import java.util.Random;

// public class Server {
//     private static final int PORT = 12345;

//     private static void ensureKeysExist() throws Exception {
//         File pub = new File("public.key");
//         File priv = new File("private.key");

//         // If both files already exist, skip generation
//         if (pub.exists() && priv.exists()) {
//             System.out.println("Keys already exist — skipping generation.");
//             return;
//         }

//         System.out.println("Generating RSA key pair...");
//         KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
//         kpg.initialize(2048);
//         KeyPair kp = kpg.generateKeyPair();

//         // Save keys in the same directory
//         try (FileOutputStream fos = new FileOutputStream(pub)) {
//             fos.write(kp.getPublic().getEncoded());
//         }
//         try (FileOutputStream fos = new FileOutputStream(priv)) {
//             fos.write(kp.getPrivate().getEncoded());
//         }

//         System.out.println("Generated public.key and private.key (DER format)");
//     }

//     public static void main(String[] args) throws Exception {

//         ensureKeysExist();
//         PrivateKey priv = loadPrivateKey("private.key");
//         System.out.println("Loaded private.key");

//         PrivateKey privateKey = loadPrivateKey("private.key");
//         System.out.println("Loaded private.key");

//         try (ServerSocket ss = new ServerSocket(PORT)) {
//             System.out.println("Server listening on port " + PORT);
//             while (true) {
//                 try (Socket s = ss.accept()) {
//                     System.out.println("Connection from " + s.getRemoteSocketAddress());
//                     handleConnection(s, privateKey);
//                 } catch (Exception ex) {
//                     ex.printStackTrace();
//                 }
//             }
//         }
//     }

//     private static void handleConnection(Socket s, PrivateKey privateKey) throws Exception {
//         InputStream in = s.getInputStream();
//         BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
//         StringBuilder sb = new StringBuilder();
//         String line;
//         // read full JSON (client sends one JSON then closes write)
//         while ((line = reader.readLine()) != null) {
//             sb.append(line);
//         }
//         String requestJson = sb.toString();
//         if (requestJson.isEmpty()) {
//             System.out.println("Empty request");
//             return;
//         }

//         Gson gson = new Gson();
//         JsonObject packet = gson.fromJson(requestJson, JsonObject.class);
//         byte[] encKey = Base64.getDecoder().decode(packet.get("enc_key").getAsString());
//         byte[] nonce = Base64.getDecoder().decode(packet.get("nonce").getAsString());
//         byte[] ct = Base64.getDecoder().decode(packet.get("ct").getAsString());

//         // RSA-OAEP decrypt AES key
//         Cipher rsa = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
//         rsa.init(Cipher.DECRYPT_MODE, privateKey);
//         byte[] aesKey = rsa.doFinal(encKey);

//         // AES-GCM decrypt
//         Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
//         GCMParameterSpec spec = new GCMParameterSpec(128, nonce);
//         SecretKey secretKey = new javax.crypto.spec.SecretKeySpec(aesKey, "AES");
//         aes.init(Cipher.DECRYPT_MODE, secretKey, spec);
//         byte[] plaintext = aes.doFinal(ct);
//         String reqStr = new String(plaintext, StandardCharsets.UTF_8);

//         // parse request
//         JsonObject req = gson.fromJson(reqStr, JsonObject.class);
//         JsonObject result = computeOperation(req);

//         // prepare response JSON
//         JsonObject respObj = new JsonObject();
//         respObj.add("result", result);
//         byte[] respBytes = gson.toJson(respObj).getBytes(StandardCharsets.UTF_8);

//         // encrypt response with same aesKey, new nonce
//         byte[] nonceResp = new byte[12];
//         new SecureRandom().nextBytes(nonceResp);
//         Cipher aesEnc = Cipher.getInstance("AES/GCM/NoPadding");
//         GCMParameterSpec spec2 = new GCMParameterSpec(128, nonceResp);
//         aesEnc.init(Cipher.ENCRYPT_MODE, secretKey, spec2);
//         byte[] ctResp = aesEnc.doFinal(respBytes);

//         JsonObject outPacket = new JsonObject();
//         outPacket.addProperty("nonce", Base64.getEncoder().encodeToString(nonceResp));
//         outPacket.addProperty("ct", Base64.getEncoder().encodeToString(ctResp));

//         // send response
//         OutputStream os = s.getOutputStream();
//         os.write(gson.toJson(outPacket).getBytes(StandardCharsets.UTF_8));
//         os.flush();
//         System.out.println("Response sent to client");
//     }

//     private static JsonObject computeOperation(JsonObject req) {
//         String op = req.get("op").getAsString();
//         Gson gson = new Gson();
//         Complex result;
//         if ("exp".equalsIgnoreCase(op)) {
//             JsonObject z = req.getAsJsonObject("z");
//             Complex cz = new Complex(z.get("re").getAsDouble(), z.get("im").getAsDouble());
//             result = cz.exp();
//         } else if ("log".equalsIgnoreCase(op)) {
//             JsonObject z = req.getAsJsonObject("z");
//             Complex cz = new Complex(z.get("re").getAsDouble(), z.get("im").getAsDouble());
//             result = cz.log();
//         } else if ("pow".equalsIgnoreCase(op)) {
//             JsonObject z1 = req.getAsJsonObject("z1");
//             JsonObject z2 = req.getAsJsonObject("z2");
//             Complex c1 = new Complex(z1.get("re").getAsDouble(), z1.get("im").getAsDouble());
//             Complex c2 = new Complex(z2.get("re").getAsDouble(), z2.get("im").getAsDouble());
//             result = c1.pow(c2);
//         } else {
//             throw new IllegalArgumentException("Unsupported op: " + op);
//         }
//         JsonObject r = new JsonObject();
//         r.addProperty("re", result.re);
//         r.addProperty("im", result.im);
//         return r;
//     }

//     private static PrivateKey loadPrivateKey(String path) throws Exception {
//         byte[] keyBytes = Files.readAllBytes(Paths.get(path));
//         PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
//         KeyFactory kf = KeyFactory.getInstance("RSA");
//         return kf.generatePrivate(spec);
//     }
// }
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// version 2.0

// The 'package' is like a folder for organizing code.
// We've put this class in the CNPROJECT.SERVER folder.
// package CNPROJECT.SERVER;

// // These are 'imports'. We're telling Java that we need to use tools
// // from other code libraries to handle things like JSON, encryption, networking, etc.
// import com.google.gson.*;
// import javax.crypto.*;
// import javax.crypto.spec.GCMParameterSpec;
// import java.io.*;
// import java.net.ServerSocket;
// import java.net.Socket;
// import java.nio.charset.StandardCharsets;
// import java.nio.file.*;
// import java.security.*;
// import java.security.spec.*;
// import java.util.Base64;

// /**
//  * The Server class listens for secure connections from clients,
//  * performs complex number calculations, and sends back an encrypted result.
//  * It's like a secure, private calculator that works over the internet.
//  */
// public class Server {
//     // This is the port number our server will listen on. Think of it like a
//     // specific
//     // apartment number at an address. The client needs to know this number to
//     // connect.
//     private static final int PORT = 12345;

//     /**
//      * This method checks if the encryption keys (our lock and master key) already
//      * exist.
//      * If they don't, it creates them. This only needs to be done once.
//      * 
//      * @throws Exception if there's an error creating the keys.
//      */
//     private static void ensureKeysExist() throws Exception {
//         File pub = new File("public.key"); // The public key file (the "lock")
//         File priv = new File("private.key"); // The private key file (the "master key")

//         // If both the lock and the key files are already there, we don't need to do
//         // anything.
//         if (pub.exists() && priv.exists()) {
//             System.out.println("Encryption keys already exist. Skipping generation.");
//             return;
//         }

//         // If keys are missing, we'll create a new pair.
//         System.out.println("Generating a new RSA key pair...");
//         KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
//         kpg.initialize(2048); // 2048 bits is a strong key size.
//         KeyPair kp = kpg.generateKeyPair();

//         // Save the public key (the lock) to a file named "public.key".
//         try (FileOutputStream fos = new FileOutputStream(pub)) {
//             fos.write(kp.getPublic().getEncoded());
//         }
//         // Save the private key (the master key) to a file named "private.key".
//         try (FileOutputStream fos = new FileOutputStream(priv)) {
//             fos.write(kp.getPrivate().getEncoded());
//         }

//         System.out.println("Successfully generated public.key and private.key.");
//     }

//     /**
//      * This is the main function where the program starts.
//      * 
//      * @param args Command line arguments (we don't use them here).
//      * @throws Exception if a major error occurs and the server has to stop.
//      */
//     public static void main(String[] args) throws Exception {
//         // First, make sure our encryption keys exist.
//         // ensureKeysExist();
//         // Load our private key (the master key) from its file. We need this to decrypt
//         // messages.
//         PrivateKey privateKey = loadPrivateKey("private.key");
//         System.out.println("Loaded private key from private.key");

//         // Start the server. The 'try-with-resources' block ensures it closes properly.
//         try (ServerSocket ss = new ServerSocket(PORT)) {
//             System.out.println("Server is up and listening on port " + PORT);
//             // This loop runs forever, waiting for new clients to connect.
//             while (true) {
//                 try (Socket s = ss.accept()) { // 'accept()' waits here until a client connects.
//                     System.out.println("Accepted a new connection from " + s.getRemoteSocketAddress());
//                     // Once a client connects, hand them off to the 'handleConnection' method.
//                     handleConnection(s, privateKey);
//                 } catch (Exception ex) {
//                     // If something goes wrong with one client, print the error but keep the server
//                     // running for others.
//                     System.err.println("Error handling a client connection: " + ex.getMessage());
//                     ex.printStackTrace();
//                 }
//             }
//         }
//     }

//     /**
//      * This method handles all the communication with a single connected client.
//      * 
//      * @param s          The client's socket (the communication channel).
//      * @param privateKey The server's private key for decryption.
//      * @throws Exception if any part of the communication or decryption fails.
//      */
//     private static void handleConnection(Socket s, PrivateKey privateKey) throws Exception {
//         // Get the stream of data coming from the client.
//         InputStream in = s.getInputStream();
//         // Read all the bytes the client sent in one go. This is safer than reading
//         // line-by-line.
//         String requestJson = new String(in.readAllBytes(), StandardCharsets.UTF_8);

//         // If the client sent nothing, we can't do anything.
//         if (requestJson.isEmpty()) {
//             System.out.println("Received an empty request. Closing connection.");
//             return;
//         }

//         // --- DECRYPTION PROCESS ---
//         // 1. Unpack the main package (the JSON object).
//         Gson gson = new Gson();
//         JsonObject packet = gson.fromJson(requestJson, JsonObject.class);
//         byte[] encKey = Base64.getDecoder().decode(packet.get("enc_key").getAsString());
//         byte[] nonce = Base64.getDecoder().decode(packet.get("nonce").getAsString());
//         byte[] ct = Base64.getDecoder().decode(packet.get("ct").getAsString());

//         // 2. Use our master key (privateKey) to unlock the small box (encKey) and get
//         // the session key (aesKey).
//         Cipher rsa = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
//         rsa.init(Cipher.DECRYPT_MODE, privateKey);
//         byte[] aesKey = rsa.doFinal(encKey);

//         // 3. Use the session key (aesKey) to unlock the main message (ct).
//         Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
//         GCMParameterSpec spec = new GCMParameterSpec(128, nonce);
//         SecretKey secretKey = new javax.crypto.spec.SecretKeySpec(aesKey, "AES");
//         aes.init(Cipher.DECRYPT_MODE, secretKey, spec);
//         byte[] plaintext = aes.doFinal(ct);
//         String reqStr = new String(plaintext, StandardCharsets.UTF_8);

//         // --- PROCESSING ---
//         // Now that we have the decrypted request, we can process it.
//         JsonObject req = gson.fromJson(reqStr, JsonObject.class);
//         JsonObject result = computeOperation(req); // Do the actual math.

//         // --- ENCRYPT AND SEND RESPONSE ---
//         // 1. Prepare the response message.
//         JsonObject respObj = new JsonObject();
//         respObj.add("result", result);
//         byte[] respBytes = gson.toJson(respObj).getBytes(StandardCharsets.UTF_8);

//         // 2. Lock the response message using the same session key, but with a new,
//         // random nonce.
//         byte[] nonceResp = new byte[12];
//         new SecureRandom().nextBytes(nonceResp);
//         Cipher aesEnc = Cipher.getInstance("AES/GCM/NoPadding");
//         GCMParameterSpec spec2 = new GCMParameterSpec(128, nonceResp);
//         aesEnc.init(Cipher.ENCRYPT_MODE, secretKey, spec2);
//         byte[] ctResp = aesEnc.doFinal(respBytes);

//         // 3. Pack the encrypted response and the new nonce into a JSON package.
//         JsonObject outPacket = new JsonObject();
//         outPacket.addProperty("nonce", Base64.getEncoder().encodeToString(nonceResp));
//         outPacket.addProperty("ct", Base64.getEncoder().encodeToString(ctResp));

//         // 4. Send the package back to the client.
//         OutputStream os = s.getOutputStream();
//         os.write(gson.toJson(outPacket).getBytes(StandardCharsets.UTF_8));
//         os.flush(); // Make sure the data is sent right away.
//         System.out.println("Encrypted response sent to client.");
//     }

//     /**
//      * The "calculator" part of the server. It reads the operation and numbers
//      * from the request and performs the correct calculation.
//      * 
//      * @param req The decrypted JSON request from the client.
//      * @return A JSON object containing the result of the calculation.
//      */
//     private static JsonObject computeOperation(JsonObject req) {
//         String op = req.get("op").getAsString();
//         Complex result;

//         // Check which operation the client wants to perform.
//         if ("exp".equalsIgnoreCase(op)) {
//             JsonObject z = req.getAsJsonObject("z");
//             Complex cz = new Complex(z.get("re").getAsDouble(), z.get("im").getAsDouble());
//             result = cz.exp();
//         } else if ("log".equalsIgnoreCase(op)) {
//             JsonObject z = req.getAsJsonObject("z");
//             Complex cz = new Complex(z.get("re").getAsDouble(), z.get("im").getAsDouble());
//             result = cz.log();
//         } else if ("pow".equalsIgnoreCase(op)) {
//             JsonObject z1 = req.getAsJsonObject("z1");
//             JsonObject z2 = req.getAsJsonObject("z2");
//             Complex c1 = new Complex(z1.get("re").getAsDouble(), z1.get("im").getAsDouble());
//             Complex c2 = new Complex(z2.get("re").getAsDouble(), z2.get("im").getAsDouble());
//             result = c1.pow(c2);
//         } else {
//             // If the operation is unknown, throw an error.
//             throw new IllegalArgumentException("Unsupported operation: " + op);
//         }

//         // Convert the final Complex number result into a JSON object to send back.
//         JsonObject r = new JsonObject();
//         r.addProperty("re", result.re);
//         r.addProperty("im", result.im);
//         return r;
//     }

//     /**
//      * A helper method to load the server's private key from a file.
//      * 
//      * @param path The path to the private key file.
//      * @return A PrivateKey object.
//      * @throws Exception if the file can't be read or the key is invalid.
//      */
//     private static PrivateKey loadPrivateKey(String path) throws Exception {
//         byte[] keyBytes = Files.readAllBytes(Paths.get(path));
//         PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
//         KeyFactory kf = KeyFactory.getInstance("RSA");
//         return kf.generatePrivate(spec);
//     }
// }

/*
 * A helper class to represent and calculate with complex numbers.
 * A complex number has a "real" part and an "imaginary" part (like 5 + 3i).
 */
// class Complex {
// final double re; // The real part
// final double im; // The imaginary part

// public Complex(double re, double im) {
// this.re = re;
// this.im = im;
// }

// // Calculates e^(this number)
// public Complex exp() {
// double expRe = Math.exp(re);
// return new Complex(expRe * Math.cos(im), expRe * Math.sin(im));
// }

// // Calculates the natural logarithm of this number
// public Complex log() {
// double modulus = Math.sqrt(re * re + im * im);
// double argument = Math.atan2(im, re);
// return new Complex(Math.log(modulus), argument);
// }

// // Calculates (this number)^(another complex number)
// public Complex pow(Complex exponent) {
// // The formula is: a^b = exp(b * log(a))
// Complex logOfBase = this.log();
// Complex product = new Complex(
// logOfBase.re * exponent.re - logOfBase.im * exponent.im,
// logOfBase.re * exponent.im + logOfBase.im * exponent.re);
// return product.exp();
// }
// }

// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0//version 3.0
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
public class Server {

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

// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// 0
// unsafe version version 4.0 (for testing only, do not use in production)

// package CNPROJECT.SERVER;

// import com.google.gson.*;
// import javax.crypto.*;
// import javax.crypto.spec.GCMParameterSpec;
// import java.io.*;
// import java.net.ServerSocket;
// import java.net.Socket;
// import java.nio.charset.StandardCharsets;
// import java.nio.file.*;
// import java.security.*;
// import java.security.spec.*;
// import java.util.Base64;
// import java.util.Random;

// /**
// * Server class that listens for secure connections, performs complex number
// * calculations,
// * and returns encrypted responses.
// */
// public class Server {

// private static final int PORT = 12345;

// public static void main(String[] args) throws Exception {
// // Ensure public/private keys exist; generate using Keygen if missing
// File pub = new File("public.key");
// File priv = new File("private.key");

// if (!pub.exists() || !priv.exists()) {
// System.out.println("Keys not found. Generating using Keygen...");
// Keygen.generateKeysIfMissing(); // This will also print keys
// } else {
// System.out.println("Keys already exist. Printing existing keys for demo:");

// // Read and print existing keys
// byte[] pubBytes = Files.readAllBytes(pub.toPath());
// byte[] privBytes = Files.readAllBytes(priv.toPath());

// System.out.println("Public Key (Base64):");
// System.out.println(Base64.getEncoder().encodeToString(pubBytes));

// System.out.println("\nPrivate Key (Base64):");
// System.out.println(Base64.getEncoder().encodeToString(privBytes));
// }

// // Load the server's private key
// PrivateKey privateKey = loadPrivateKey("private.key");
// System.out.println("Loaded private key from private.key");

// // Start the server socket
// try (ServerSocket ss = new ServerSocket(PORT)) {
// System.out.println("Server is up and listening on port " + PORT);

// while (true) {
// try (Socket s = ss.accept()) {
// System.out.println("Accepted a connection from " +
// s.getRemoteSocketAddress());
// handleConnection(s, privateKey);
// } catch (Exception ex) {
// System.err.println("Error handling client: " + ex.getMessage());
// ex.printStackTrace();
// }
// }
// }
// }

// /**
// * Handles communication with a connected client.
// */
// private static void handleConnection(Socket s, PrivateKey privateKey) throws
// Exception {
// InputStream in = s.getInputStream();
// String requestJson = new String(in.readAllBytes(), StandardCharsets.UTF_8);

// if (requestJson.isEmpty()) {
// System.out.println("Received empty request. Closing connection.");
// return;
// }

// Gson gson = new Gson();
// JsonObject packet = gson.fromJson(requestJson, JsonObject.class);

// byte[] encKey =
// Base64.getDecoder().decode(packet.get("enc_key").getAsString());
// byte[] nonce = Base64.getDecoder().decode(packet.get("nonce").getAsString());
// byte[] ct = Base64.getDecoder().decode(packet.get("ct").getAsString());

// // RSA-OAEP decrypt AES session key
// Cipher rsa = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
// rsa.init(Cipher.DECRYPT_MODE, privateKey);
// byte[] aesKey = rsa.doFinal(encKey);

// // AES-GCM decrypt payload
// Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
// GCMParameterSpec spec = new GCMParameterSpec(128, nonce);
// SecretKey secretKey = new javax.crypto.spec.SecretKeySpec(aesKey, "AES");
// aes.init(Cipher.DECRYPT_MODE, secretKey, spec);
// byte[] plaintext = aes.doFinal(ct);

// JsonObject req = gson.fromJson(new String(plaintext, StandardCharsets.UTF_8),
// JsonObject.class);
// JsonObject result = computeOperation(req);

// // Encrypt response
// JsonObject respObj = new JsonObject();
// respObj.add("result", result);
// byte[] respBytes = gson.toJson(respObj).getBytes(StandardCharsets.UTF_8);

// byte[] nonceResp = new byte[12];
// new SecureRandom().nextBytes(nonceResp);
// Cipher aesEnc = Cipher.getInstance("AES/GCM/NoPadding");
// GCMParameterSpec spec2 = new GCMParameterSpec(128, nonceResp);
// aesEnc.init(Cipher.ENCRYPT_MODE, secretKey, spec2);
// byte[] ctResp = aesEnc.doFinal(respBytes);

// JsonObject outPacket = new JsonObject();
// outPacket.addProperty("nonce",
// Base64.getEncoder().encodeToString(nonceResp));
// outPacket.addProperty("ct", Base64.getEncoder().encodeToString(ctResp));

// OutputStream os = s.getOutputStream();
// os.write(gson.toJson(outPacket).getBytes(StandardCharsets.UTF_8));
// os.flush();
// System.out.println("Encrypted response sent to client.");
// }

// /**
// * Computes the requested complex number operation using the external Complex
// * class.
// */
// private static JsonObject computeOperation(JsonObject req) {
// String op = req.get("op").getAsString();
// Complex result;

// switch (op.toLowerCase()) {
// case "exp": {
// JsonObject z = req.getAsJsonObject("z");
// Complex cz = new Complex(z.get("re").getAsDouble(),
// z.get("im").getAsDouble());
// result = cz.exp();
// break;
// }
// case "log": {
// JsonObject z = req.getAsJsonObject("z");
// Complex cz = new Complex(z.get("re").getAsDouble(),
// z.get("im").getAsDouble());
// result = cz.log();
// break;
// }
// case "pow": {
// JsonObject z1 = req.getAsJsonObject("z1");
// JsonObject z2 = req.getAsJsonObject("z2");
// Complex c1 = new Complex(z1.get("re").getAsDouble(),
// z1.get("im").getAsDouble());
// Complex c2 = new Complex(z2.get("re").getAsDouble(),
// z2.get("im").getAsDouble());
// result = c1.pow(c2);
// break;
// }
// default:
// throw new IllegalArgumentException("Unsupported operation: " + op);
// }

// JsonObject r = new JsonObject();
// r.addProperty("re", result.re);
// r.addProperty("im", result.im);
// return r;
// }

// /**
// * Loads the server's private key from a DER file.
// */
// private static PrivateKey loadPrivateKey(String path) throws Exception {
// byte[] keyBytes = Files.readAllBytes(Paths.get(path));
// PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
// KeyFactory kf = KeyFactory.getInstance("RSA");
// return kf.generatePrivate(spec);
// }
// }
