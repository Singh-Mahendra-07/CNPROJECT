// package CNPROJECT.CLIENT;
// The package declaration defines the namespace for this class, helping organize code files logically

// This file belongs to the CLIENT module of the CNPROJECT package

// Client.java
// This file likely implements the client-side functionality of a network application (e.g., chat or secure communication)

// Importing libraries
// import com.google.gson.*; // Provides classes for converting Java objects to/from JSON (used for message encoding/decoding)

// import javax.crypto.*; // Provides cryptographic operations such as encryption, decryption, key generation
// import javax.crypto.spec.GCMParameterSpec; // Used for AES-GCM (Galois/Counter Mode) encryption, ensuring data confidentiality and integrity

// import java.io.*; // For input/output operations (reading/writing data streams and files)
// import java.net.Socket; // For creating and managing TCP socket connections (client-server communication)
// import java.nio.charset.StandardCharsets; // Provides standard character encodings (e.g., UTF-8)
// import java.nio.file.*; // Used for file operations (reading/writing files, managing paths)
// import java.security.*; // Contains cryptography-related classes (e.g., KeyPair, MessageDigest, SecureRandom)
// import java.security.spec.*; // Provides specifications for cryptographic key formats and algorithms
// import java.util.Base64; // For encoding/decoding binary data (e.g., keys or encrypted messages) into a text-friendly Base64 format
// import java.util.Scanner; // For reading user input from the console

// import com.google.gson.Gson;
// import com.google.gson.JsonObject;

// import javax.crypto.Cipher;
// import javax.crypto.SecretKey;
// import javax.crypto.spec.GCMParameterSpec;
// import java.io.InputStream;
// import java.io.OutputStream;
// import java.net.Socket;
// import java.nio.charset.StandardCharsets;
// import java.nio.file.Files;
// import java.nio.file.Paths;
// import java.security.KeyFactory;
// import java.security.PublicKey;
// import java.security.SecureRandom;
// import java.security.spec.X509EncodedKeySpec;
// import java.util.Base64;
// import java.util.Scanner;

/**
 * Client
 *
 * <p>Command-line client that constructs a JSON request representing a complex-number operation,
 * encrypts the request using hybrid encryption (AES-GCM for payload + RSA-OAEP for the AES key),
 * sends the encrypted packet to a remote server over a TCP socket, and receives & decrypts the
 * server's encrypted response.</p>
 *
 * <p>Behavior overview:
 * <ol>
 *   <li>Prompt the user to choose an operation: "exp", "log" or "pow".</li>
 *   <li>Read the required complex-number operands from standard input:
 *     <ul>
 *       <li>exp, log: a single complex number z with fields "re" and "im".</li>
 *       <li>pow: two complex numbers z1 and z2 each with "re" and "im".</li>
 *     </ul>
 *   </li>
 *   <li>Serialize the request to JSON (using Gson) with top-level field "op" and either "z"
 *       or "z1" and "z2".</li>
 *   <li>Generate a random 32-byte AES key (AES-256) and a 12-byte nonce, encrypt the JSON with
 *       AES/GCM/NoPadding (128-bit authentication tag).</li>
 *   <li>Encrypt the AES key using RSA/ECB/OAEPWithSHA-256AndMGF1Padding with the server's RSA
 *       public key loaded from the given file.</li>
 *   <li>Send a JSON packet containing base64-encoded fields:
 *       "enc_key" (RSA-encrypted AES key), "nonce" (AES-GCM nonce), and "ct" (ciphertext + tag).</li>
 *   <li>Read the server's response packet (same envelope), base64-decode fields, decrypt the AES-GCM
 *       ciphertext with the original AES key and nonce, and print the resulting server JSON payload.</li>
 * </ol>
 * </p>
 *
 * <p>Request JSON examples:
 * <pre>
 *   { "op":"exp", "z": { "re": 1.0, "im": 2.0 } }
 *   { "op":"log", "z": { "re": 1.0, "im": -1.0 } }
 *   { "op":"pow", "z1": { "re": 1.0, "im": 0.0 }, "z2": { "re": 0.0, "im": 1.0 } }
 * </pre>
 * Server/response payloads are application-specific JSON strings encrypted with the same AES key.
 * </p>
 *
 * <p>Network & crypto details:
 * <ul>
 *   <li>Host/port are defined by HOST and PORT constants (defaults in source: 127.0.0.1:12345).</li>
 *   <li>Hybrid encryption: AES-256/GCM for confidentiality+integrity and RSA-OAEP-SHA256 for the
 *       symmetric key transport.</li>
 *   <li>GCM nonce length is 12 bytes and authentication tag is 128 bits.</li>
 *   <li>RSA public key must be provided in X.509 SubjectPublicKeyInfo DER encoding (raw bytes read
 *       from file). If the key is in PEM format it must be converted/decoded to DER before use.</li>
 * </ul>
 * </p>
 *
 * <p>Assumptions & prerequisites:
 * <ul>
 *   <li>A server is running and listening on HOST:PORT implementing the same envelope and crypto
 *       conventions.</li>
 *   <li>The file "public.key" (or provided path) contains the server RSA public key in X.509/DER form.</li>
 *   <li>Gson library is available at runtime for JSON (the code uses gson.toJson / fromJson).</li>
 * </ul>
 * </p>
 *
 * <p>Security considerations & notes:
 * <ul>
 *   <li>SecureRandom is used to generate AES keys and nonces, which is appropriate; ensure the
 *       platform provides a secure PRNG.</li>
 *   <li>Reusing the same AES key/nonce pair for multiple messages is insecure; the client here
 *       generates a fresh key and nonce per request (hybrid model) which is good.</li>
 *   <li>The RSA key-wrapping uses OAEP with SHA-256 (recommended over PKCS#1 v1.5).</li>
 *   <li>The code reads the entire socket response via readAllBytes(); for large responses or
 *       streaming scenarios consider a length-prefix or chunked protocol to avoid buffering issues.</li>
 * </ul>
 * </p>
 *
 * <p>Limitations & potential improvements:
 * <ul>
 *   <li>Input parsing uses Scanner.nextDouble() which can throw InputMismatchException for malformed
 *       input; add validation and retry prompts for a better UX.</li>
 *   <li>Error handling is minimal: main throws Exception. Consider catching and handling specific
 *       exceptions to provide friendlier messages and to ensure resources are always released.</li>
 *   <li>The client sends a raw JSON string over TCP without application framing. If the server expects
 *       framing (e.g., newline or length prefix) adapt the protocol accordingly.</li>
 *   <li>Currently the client closes System.in Scanner which will also close the underlying input
 *       stream; if additional input is needed elsewhere this may be undesirable.</li>
 * </ul>
 * </p>
 */

/**
 * main(String[] args)
 *
 * <p>Interactive entry point. Prompts the user for a complex-number operation and operands,
 * constructs a JSON request, encrypts it using a freshly generated AES-256/GCM key and a server
 * RSA public key (RSA-OAEP-SHA256), sends the encrypted envelope to the configured HOST:PORT, then
 * reads and decrypts the server's encrypted response and prints the resulting JSON payload.</p>
 *
 * <p>Side effects:
 * <ul>
 *   <li>Reads from standard input (Scanner).</li>
 *   <li>Opens a TCP connection to HOST:PORT and writes/reads bytes.</li>
 *   <li>Prints status and the decrypted server response to standard output.</li>
 * </ul>
 * </p>
 *
 * @param args command-line arguments (not used)
 * @throws Exception propagated for any I/O, JSON, or crypto errors (caller should handle or allow JVM to exit)
 */

/**
 * loadPublicKey(String path)
 *
 * <p>
 * Load an RSA public key from a file. The implementation expects the file to
 * contain the X.509
 * SubjectPublicKeyInfo encoded public key bytes (DER). The method reads the raw
 * bytes and constructs
 * an RSA PublicKey via X509EncodedKeySpec.
 * </p>
 *
 * <p>
 * If you have a PEM-formatted key (-----BEGIN PUBLIC KEY----- ...), strip the
 * header/footer and
 * Base64-decode the body to obtain the DER bytes before calling this method or
 * adapt the method to
 * perform PEM decoding.
 * </p>
 *
 * @param path filesystem path to the public key file (DER-encoded X.509
 *             SubjectPublicKeyInfo)
 * @return a java.security.PublicKey instance representing the RSA public key
 * @throws Exception if the file cannot be read or the bytes do not represent a
 *                   valid RSA public key
 */

// version 1
// public class Client {
// private static final String HOST = "127.0.0.1";
// private static final int PORT = 12345;

// public static void main(String[] args) throws Exception {
// PublicKey pub = loadPublicKey("public.key");
// System.out.println("Loaded public.key");

// Gson gson = new Gson();
// Scanner sc = new Scanner(System.in);

// // ======== Step 1: Take user input ========
// System.out.println("Choose operation (exp / log / pow): ");
// String op = sc.nextLine();

// JsonObject req = new JsonObject();
// req.addProperty("op", op);

// if (op.equalsIgnoreCase("exp") || op.equalsIgnoreCase("log")) {
// System.out.println("Enter real part: ");
// double re = sc.nextDouble();
// System.out.println("Enter imaginary part: ");
// double im = sc.nextDouble();
// JsonObject z = new JsonObject();
// z.addProperty("re", re);
// z.addProperty("im", im);
// req.add("z", z);
// } else if (op.equalsIgnoreCase("pow")) {
// System.out.println("Enter real part of base (z1.re): ");
// double re1 = sc.nextDouble();
// System.out.println("Enter imaginary part of base (z1.im): ");
// double im1 = sc.nextDouble();

// System.out.println("Enter real part of exponent (z2.re): ");
// double re2 = sc.nextDouble();
// System.out.println("Enter imaginary part of exponent (z2.im): ");
// double im2 = sc.nextDouble();

// JsonObject z1 = new JsonObject();
// z1.addProperty("re", re1);
// z1.addProperty("im", im1);
// JsonObject z2 = new JsonObject();
// z2.addProperty("re", re2);
// z2.addProperty("im", im2);
// req.add("z1", z1);
// req.add("z2", z2);
// } else {
// System.out.println("Invalid operation! Use exp, log, or pow.");
// sc.close();
// return;
// }

// sc.close();
// System.out.println("Request JSON: " + gson.toJson(req));

// // ======== Step 2: Encryption setup ========
// byte[] aesKey = new byte[32]; // AES-256 key
// new SecureRandom().nextBytes(aesKey);
// byte[] nonce = new byte[12];
// new SecureRandom().nextBytes(nonce);

// // AES-GCM encrypt the JSON request
// SecretKey secretKey = new javax.crypto.spec.SecretKeySpec(aesKey, "AES");
// Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
// GCMParameterSpec spec = new GCMParameterSpec(128, nonce);
// aes.init(Cipher.ENCRYPT_MODE, secretKey, spec);
// byte[] plaintext = gson.toJson(req).getBytes(StandardCharsets.UTF_8);
// byte[] ct = aes.doFinal(plaintext);

// // RSA-OAEP encrypt AES key using the server's public key
// Cipher rsa = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
// rsa.init(Cipher.ENCRYPT_MODE, pub);
// byte[] encKey = rsa.doFinal(aesKey);

// // Build packet JSON {enc_key, nonce, ct}
// JsonObject packet = new JsonObject();
// packet.addProperty("enc_key", Base64.getEncoder().encodeToString(encKey));
// packet.addProperty("nonce", Base64.getEncoder().encodeToString(nonce));
// packet.addProperty("ct", Base64.getEncoder().encodeToString(ct));
// String packetStr = gson.toJson(packet);

// // ======== Step 3: Send request and receive response ========
// try (Socket s = new Socket(HOST, PORT)) {
// OutputStream os = s.getOutputStream();
// os.write(packetStr.getBytes(StandardCharsets.UTF_8));
// os.flush();
// s.shutdownOutput(); // signal end of request

// // Read response from server
// InputStream in = s.getInputStream();
// String respStr = new String(in.readAllBytes(), StandardCharsets.UTF_8);
// JsonObject respPacket = gson.fromJson(respStr, JsonObject.class);

// byte[] nonceResp =
// Base64.getDecoder().decode(respPacket.get("nonce").getAsString());
// byte[] ctResp =
// Base64.getDecoder().decode(respPacket.get("ct").getAsString());

// // Decrypt response with same AES key
// Cipher aesDec = Cipher.getInstance("AES/GCM/NoPadding");
// GCMParameterSpec spec2 = new GCMParameterSpec(128, nonceResp);
// aesDec.init(Cipher.DECRYPT_MODE, secretKey, spec2);
// byte[] ptResp = aesDec.doFinal(ctResp);
// String payload = new String(ptResp, StandardCharsets.UTF_8);

// System.out.println("Server response JSON: " + payload);
// }
// }

// private static PublicKey loadPublicKey(String path) throws Exception {
// byte[] keyBytes = Files.readAllBytes(Paths.get(path));
// X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
// KeyFactory kf = KeyFactory.getInstance("RSA");
// return kf.generatePublic(spec);
// }
// }

// version 2

// public class Client {
//     // Define the server's IP address. "127.0.0.1" is the standard address for
//     // localhost,
//     // meaning the server is expected to be running on the same machine as the
//     // client.
//     private static final String HOST = "127.0.0.1";
//     // Define the port number for the server communication. Both client and server
//     // must be configured to use the same port.
//     private static final int PORT = 11243;

//     /**
//      * The main method, which is the entry point of the application.
//      * 
//      * @param args Command-line arguments (not used in this application).
//      * @throws Exception Catches and throws any potential errors related to I/O,
//      *                   networking, or cryptography.
//      */
//     public static void main(String[] args) throws Exception {
//         // Load the server's RSA public key from the "public.key" file.
//         // This key is used to encrypt the symmetric AES key, ensuring only the server
//         // can read it.
//         PublicKey pub = loadPublicKey("public.key");
//         System.out.println("Loaded server's public key from public.key");

//         // Initialize Gson, a library for converting Java objects to and from JSON
//         // format.
//         Gson gson = new Gson();
//         // Initialize a Scanner to read input from the user via the console.
//         Scanner sc = new Scanner(System.in);

//         // ======== Step 1: Get User Input ========
//         // Prompt the user to choose a mathematical operation.
//         System.out.println("Choose operation (exp / log / pow / add / sub / mul / div): ");
//         String op = sc.nextLine();

//         // Create a new JSON object to build the request payload.
//         JsonObject req = new JsonObject();
//         // Add the chosen operation to the request object.
//         req.addProperty("op", op);

//         // Collect the required numbers based on the chosen operation.
//         if (op.equalsIgnoreCase("exp") || op.equalsIgnoreCase("log")) {
//             // For 'exp' and 'log', one complex number is needed.
//             System.out.println("Enter real part: ");
//             double re = sc.nextDouble();
//             System.out.println("Enter imaginary part: ");
//             double im = sc.nextDouble();
//             JsonObject z = new JsonObject();
//             z.addProperty("re", re);
//             z.addProperty("im", im);
//             req.add("z", z);
//         } else if (op.equalsIgnoreCase("pow")) {
//             // For 'pow', two complex numbers are needed (base and exponent).
//             System.out.println("Enter real part of base (z1.re): ");
//             double re1 = sc.nextDouble();
//             System.out.println("Enter imaginary part of base (z1.im): ");
//             double im1 = sc.nextDouble();

//             System.out.println("Enter real part of exponent (z2.re): ");
//             double re2 = sc.nextDouble();
//             System.out.println("Enter imaginary part of exponent (z2.im): ");
//             double im2 = sc.nextDouble();

//             // Create JSON objects for both complex numbers and add them to the request.
//             JsonObject z1 = new JsonObject();
//             z1.addProperty("re", re1);
//             z1.addProperty("im", im1);
//             JsonObject z2 = new JsonObject();
//             z2.addProperty("re", re2);
//             z2.addProperty("im", im2);
//             req.add("z1", z1);
//             req.add("z2", z2);
//         } else {
//             // If the user enters an unsupported operation, show an error and exit.
//             System.out.println("Invalid operation! Use exp, log, or pow.");
//             sc.close();
//             return;
//         }

//         // Close the scanner since we have all the required input.
//         sc.close();
//         // Display the raw JSON request before it gets encrypted.
//         System.out.println("Request JSON (before encryption): " + gson.toJson(req));

//         // ======== Step 2: Set Up and Perform Encryption ========
//         // Generate a cryptographically secure random 32-byte (256-bit) AES key.
//         // This key will be used for the fast symmetric encryption of the request data.
//         byte[] aesKey = new byte[32];
//         new SecureRandom().nextBytes(aesKey);
//         // Generate a 12-byte random nonce (number used once). A unique nonce is crucial
//         // for the security of the AES-GCM encryption mode.
//         byte[] nonce = new byte[12];
//         new SecureRandom().nextBytes(nonce);

//         // Encrypt the JSON request string using AES in GCM mode. GCM provides both
//         // confidentiality (encryption) and authenticity, protecting against tampering.
//         SecretKey secretKey = new javax.crypto.spec.SecretKeySpec(aesKey, "AES");
//         Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
//         GCMParameterSpec spec = new GCMParameterSpec(128, nonce); // 128-bit authentication tag size
//         aes.init(Cipher.ENCRYPT_MODE, secretKey, spec);
//         byte[] plaintext = gson.toJson(req).getBytes(StandardCharsets.UTF_8);
//         byte[] ct = aes.doFinal(plaintext); // 'ct' is the resulting ciphertext (encrypted data).

//         // Encrypt the randomly generated AES key using the server's public RSA key.
//         // This is the "hybrid" part of hybrid encryption. It allows us to securely
//         // share the secret AES key with the server.
//         Cipher rsa = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
//         rsa.init(Cipher.ENCRYPT_MODE, pub);
//         byte[] encKey = rsa.doFinal(aesKey);

//         // Assemble the final packet to be sent to the server.
//         // This packet contains the encrypted AES key, the nonce, and the ciphertext.
//         // All byte arrays are encoded into Base64 strings, which is a safe way to
//         // transmit binary data within a JSON text format.
//         JsonObject packet = new JsonObject();
//         packet.addProperty("enc_key", Base64.getEncoder().encodeToString(encKey));
//         packet.addProperty("nonce", Base64.getEncoder().encodeToString(nonce));
//         packet.addProperty("ct", Base64.getEncoder().encodeToString(ct));
//         String packetStr = gson.toJson(packet);

//         // ======== Step 3: Send the Request and Receive the Response ========
//         // Use a try-with-resources statement to ensure the socket is automatically
//         // closed after use.
//         try (Socket s = new Socket(HOST, PORT)) {
//             // Get the socket's output stream to send data to the server.
//             OutputStream os = s.getOutputStream();
//             os.write(packetStr.getBytes(StandardCharsets.UTF_8));
//             os.flush(); // Ensure all buffered data is sent immediately.
//             s.shutdownOutput(); // Signal to the server that the request has been fully sent.

//             // Read the encrypted response back from the server.
//             InputStream in = s.getInputStream();
//             String respStr = new String(in.readAllBytes(), StandardCharsets.UTF_8);
//             JsonObject respPacket = gson.fromJson(respStr, JsonObject.class);

//             // Extract the nonce and ciphertext from the server's response packet, decoding
//             // them from Base64.
//             byte[] nonceResp = Base64.getDecoder().decode(respPacket.get("nonce").getAsString());
//             byte[] ctResp = Base64.getDecoder().decode(respPacket.get("ct").getAsString());

//             // Decrypt the server's response using the *same* AES key that we generated
//             // earlier.
//             Cipher aesDec = Cipher.getInstance("AES/GCM/NoPadding");
//             GCMParameterSpec spec2 = new GCMParameterSpec(128, nonceResp);
//             aesDec.init(Cipher.DECRYPT_MODE, secretKey, spec2);
//             byte[] ptResp = aesDec.doFinal(ctResp); // 'ptResp' is the decrypted plaintext.
//             String payload = new String(ptResp, StandardCharsets.UTF_8);

//             // Print the final decrypted result from the server.
//             System.out.println("Server response JSON: " + payload);
//         }
//     }

//     /**
//      * Helper method to load an RSA public key from a file on disk.
//      * 
//      * @param path The path to the public key file.
//      * @return A PublicKey object ready for use in encryption.
//      * @throws Exception If there's an issue reading the file or parsing the key.
//      */
//     private static PublicKey loadPublicKey(String path) throws Exception {
//         // Read all bytes from the specified file path.
//         byte[] keyBytes = Files.readAllBytes(Paths.get(path));
//         // Create a key specification based on the X.509 standard format for public
//         // keys.
//         X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
//         // Get a KeyFactory instance for the RSA algorithm.
//         KeyFactory kf = KeyFactory.getInstance("RSA");
//         // Generate and return the PublicKey object from the specification.
//         return kf.generatePublic(spec);
//     }
// }

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
public class Client {
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
