package tech.edgx;

import com.google.gson.Gson;
import io.libp2p.core.PeerId;
import io.libp2p.core.crypto.KeyKt;
import io.libp2p.core.crypto.PrivKey;
import io.libp2p.crypto.keys.Ed25519Kt;
import org.bouncycastle.util.encoders.Base64;
import org.junit.Test;
import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import java.security.AlgorithmParameters;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;

import org.peergos.HostBuilder;
import org.peergos.config.IdentitySection;
import org.peergos.util.JsonHelper;
import tech.edgx.util.HexUtil;

public class CryptoTests {

    @Test
    public void generateKeys() throws Exception {

        PrivKey privKey = Ed25519Kt.generateEd25519KeyPair().getFirst();
        //    return setPrivKey(KeyKt.unmarshalPrivateKey(privKey));

        System.out.println("PrivKey (Base64): "+ io.ipfs.multibase.binary.Base64.encodeBase64String(privKey.bytes()));

    }

    @Test
    public void generateKeyPairOrganic() throws Exception {
        HostBuilder hostBuilder = new HostBuilder();
        hostBuilder = hostBuilder.generateIdentity();
        System.out.println("PrivKey: "+ new Gson().toJson(hostBuilder.getPrivateKey()));
        System.out.println("PubKey: "+ new Gson().toJson(hostBuilder.getPrivateKey().publicKey()));

        System.out.println("PrivKey Type: "+ hostBuilder.getPrivateKey().getKeyType()); // Ed25519
        System.out.println("PubKey Type: "+ hostBuilder.getPrivateKey().publicKey().getKeyType()); // Ed25519

        System.out.println("PrivKey Hex: "+ HexUtil.encodeHexString(hostBuilder.getPrivateKey().bytes())); // Not equiv format
        System.out.println("PrivKey Hex (raw): "+ HexUtil.encodeHexString(hostBuilder.getPrivateKey().raw())); // Not equiv format

        // IPFS uses base64 encoded priv keys
        System.out.println("PrivKey (Base64): "+ io.ipfs.multibase.binary.Base64.encodeBase64String(hostBuilder.getPrivateKey().raw()));

        // IPFS uses base58 encoded pubkey to get the peerid
        System.out.println("PeerId: "+ PeerId.fromPubKey(hostBuilder.getPrivateKey().publicKey()).toBase58());
        System.out.println("PeerId2: "+ hostBuilder.getPeerId().toBase58());


        //REF:
//        String base64PrivKey = JsonHelper.getStringProperty(json, "Identity", "PrivKey");
//        byte[] privKey = io.ipfs.multibase.binary.Base64.decodeBase64(base64PrivKey);
//        String base58PeerID = JsonHelper.getStringProperty(json, "Identity", "PeerID");
//        return new IdentitySection(privKey, PeerId.fromBase58(base58PeerID));

        //CAESQBtQ544kB+kA1ZDyaYY4f4ZnZEj4c8O7iXUZob8+rlRllmB9VloN+cA5Quoyocg5JTmKuo6efCfhguCDCo3JITQ="
//        byte[] decodedExPrivKey = io.ipfs.multibase.binary.Base64.decodeBase64("CAESICmecDYijP+SzFJgQ1DqJb0VC/jFA8CZBAH/JoI9khJ6");
        byte[] decodedExPrivKey = io.ipfs.multibase.binary.Base64.decodeBase64("CAESQBtQ544kB+kA1ZDyaYY4f4ZnZEj4c8O7iXUZob8+rlRllmB9VloN+cA5Quoyocg5JTmKuo6efCfhguCDCo3JITQ=");
        System.out.println("Decodedpriv key: "+new Gson().toJson(decodedExPrivKey));
        System.out.println("ReEncoded PrivKey (Base64): "+ io.ipfs.multibase.binary.Base64.encodeBase64String(decodedExPrivKey));

    }

    /* Trying to gen keypair from first principles - DEPRECATED */
    @Test
    public void generateKeyPair() throws Exception {
        // generate key pair

        // e.g. it should be RSA-2048 keypair
//        jsipfs init
//        initializing ipfs node at /Users/pascalprecht/.jsipfs
//        generating 2048-bit RSA keypair...done
//        peer identity: QmYDkVX6kUFrn8FKiDKrFqhrkbr4Ax1nxxvgJfT5C6feXv
//        to get started, enter:
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.genKeyPair();

        // TODO, get a NODE id from this keypair????

        // extract the encoded private key, this is an unencrypted PKCS#8 private key
        byte[] encodedprivkey = keyPair.getPrivate().getEncoded();
        System.out.println("Encoded Key: "+ HexUtil.encodeHexString(encodedprivkey));
        System.out.println("PrivKey: "+ keyPair.getPrivate());
        //System.out.println("PrivKey: "+ new Gson().toJson(keyPair.getPrivate()));




// We must use a PasswordBasedEncryption algorithm in order to encrypt the private key, you may use any common algorithm supported by openssl, you can check them in the openssl documentation http://www.openssl.org/docs/apps/pkcs8.html
        String MYPBEALG = "PBEWithSHA1AndDESede";
        String password = "pleaseChangeit!";

        int count = 20;// hash iteration count
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[8];
        random.nextBytes(salt);

// Create PBE parameter set
        PBEParameterSpec pbeParamSpec = new PBEParameterSpec(salt, count);
        PBEKeySpec pbeKeySpec = new PBEKeySpec(password.toCharArray());
        SecretKeyFactory keyFac = SecretKeyFactory.getInstance(MYPBEALG);
        SecretKey pbeKey = keyFac.generateSecret(pbeKeySpec);

        Cipher pbeCipher = Cipher.getInstance(MYPBEALG);

// Initialize PBE Cipher with key and parameters
        pbeCipher.init(Cipher.ENCRYPT_MODE, pbeKey, pbeParamSpec);

// Encrypt the encoded Private Key with the PBE key
        byte[] ciphertext = pbeCipher.doFinal(encodedprivkey);

// Now construct  PKCS #8 EncryptedPrivateKeyInfo object
        AlgorithmParameters algparms = AlgorithmParameters.getInstance(MYPBEALG);
        algparms.init(pbeParamSpec);
        EncryptedPrivateKeyInfo encinfo = new EncryptedPrivateKeyInfo(algparms, ciphertext);

// and here we have it! a DER encoded PKCS#8 encrypted key!
        byte[] encryptedPkcs8 = encinfo.getEncoded();

        System.out.println("Key: "+ HexUtil.encodeHexString(encryptedPkcs8));
    }
}
