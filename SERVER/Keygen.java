// package CNPROJECT.SERVER;

// // KeyGen.java
// import java.io.FileOutputStream;
// import java.io.IOException;
// import java.security.*;

// public class Keygen {
//     public static void main(String[] args) throws Exception {
//         KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
//         kpg.initialize(2048);
//         KeyPair kp = kpg.generateKeyPair();

//         // Write public (X.509) and private (PKCS#8) DER files
//         try (FileOutputStream fos = new FileOutputStream("public.key")) {
//             fos.write(kp.getPublic().getEncoded());
//         }
//         try (FileOutputStream fos = new FileOutputStream("private.key")) {
//             fos.write(kp.getPrivate().getEncoded());
//         }
//         System.out.println("Generated public.key and private.key (DER format)");
//     }
// }

//version 2.0
package CNPROJECT.SERVER;

import java.io.FileOutputStream;
import java.io.File;
import java.security.*;

public class Keygen {

    // Static method to generate keys if missing
    public static void generateKeysIfMissing() throws Exception {
        File pub = new File("public.key");
        File priv = new File("private.key");

        if (pub.exists() && priv.exists()) {
            System.out.println("Keys already exist. Skipping generation.");
            return;
        }

        System.out.println("Generating RSA key pair...");
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();

        try (FileOutputStream fos = new FileOutputStream(pub)) {
            fos.write(kp.getPublic().getEncoded());
        }
        try (FileOutputStream fos = new FileOutputStream(priv)) {
            fos.write(kp.getPrivate().getEncoded());
        }

        System.out.println("Generated public.key and private.key successfully.");
    }

    // Optional: you can still keep main() to generate keys manually
    public static void main(String[] args) throws Exception {
        generateKeysIfMissing();
    }
}

// version 3.0
// package CNPROJECT.SERVER;

// import java.io.FileOutputStream;
// import java.io.File;
// import java.security.*;
// import java.util.Base64;

// public class Keygen {

// // Static method to generate keys if missing
// public static void generateKeysIfMissing() throws Exception {
// File pub = new File("public.key");
// File priv = new File("private.key");

// if (pub.exists() && priv.exists()) {
// System.out.println("Keys already exist. Skipping generation.");
// return;
// }

// System.out.println("Generating RSA key pair...");
// KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
// kpg.initialize(2048);
// KeyPair kp = kpg.generateKeyPair();

// // Save public key
// try (FileOutputStream fos = new FileOutputStream(pub)) {
// fos.write(kp.getPublic().getEncoded());
// }
// // Save private key
// try (FileOutputStream fos = new FileOutputStream(priv)) {
// fos.write(kp.getPrivate().getEncoded());
// }

// // Print keys for demonstration
// System.out.println("Public Key (Base64):");
// System.out.println(Base64.getEncoder().encodeToString(kp.getPublic().getEncoded()));

// System.out.println("\nPrivate Key (Base64):");
// // For cleaner output, shorten the private key print
// String privKeyBase64 =
// Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded());
// System.out.println(privKeyBase64);

// System.out.println("\nGenerated public.key and private.key successfully.");
// }

// // Optional: manual generation
// public static void main(String[] args) throws Exception {
// generateKeysIfMissing();
// }
// }
