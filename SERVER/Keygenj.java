// package CNPROJECT.SERVER;

// import java.io.File;
// import java.io.FileOutputStream;
// import java.security.KeyPair;
// import java.security.KeyPairGenerator;

// public class Keygenj {

//     public static void main(String[] args) {
//         try {
//             File pub = new File("public.key");
//             File priv = new File("private.key");

//             // check if files already exist
//             if (pub.exists() && priv.exists()) {
//                 System.out.println("Keys already exist!");
//                 return;
//             }

//             System.out.println("Making new RSA key pair...");
//             KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
//             gen.initialize(2048);
//             KeyPair pair = gen.generateKeyPair();

//             // write public key
//             FileOutputStream f1 = new FileOutputStream("public.key");
//             f1.write(pair.getPublic().getEncoded());
//             f1.close();

//             // write private key
//             FileOutputStream f2 = new FileOutputStream("private.key");
//             f2.write(pair.getPrivate().getEncoded());
//             f2.close();

//             System.out.println("public.key and private.key created!");
//         } catch (Exception e) {
//             System.out.println("Something went wrong while generating keys!");
//             e.printStackTrace();
//         }
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

//version 2.0
package CNPROJECT.SERVER;

import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;

public class Keygenj {

    public static void generateKeysIfMissing() {
        try {
            File pub = new File("public.key");
            File priv = new File("private.key");

            // check if files already exist
            if (pub.exists() && priv.exists()) {
                System.out.println("Keys already exist!");
                return;
            }

            System.out.println("Making new RSA key pair...");
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            KeyPair pair = gen.generateKeyPair();

            // write public key
            FileOutputStream f1 = new FileOutputStream("public.key");
            f1.write(pair.getPublic().getEncoded());
            f1.close();

            // write private key
            FileOutputStream f2 = new FileOutputStream("private.key");
            f2.write(pair.getPrivate().getEncoded());
            f2.close();

            System.out.println("public.key and private.key created!");
        } catch (Exception e) {
            System.out.println("Something went wrong while generating keys!");
            e.printStackTrace();
        }
    }

    // optional main() if you want to run independently
    public static void main(String[] args) {
        generateKeysIfMissing();
    }
}