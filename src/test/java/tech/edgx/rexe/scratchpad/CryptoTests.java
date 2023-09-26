package tech.edgx.rexe.scratchpad;

import com.google.gson.Gson;
import io.libp2p.core.PeerId;
import io.libp2p.core.crypto.PrivKey;
import io.libp2p.crypto.keys.Ed25519Kt;
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
import tech.edgx.rexe.util.HexUtil;

public class CryptoTests {

    @Test
    public void generateKeys() throws Exception {
        PrivKey privKey = Ed25519Kt.generateEd25519KeyPair().getFirst();
        System.out.println("PrivKey (Base64): "+ io.ipfs.multibase.binary.Base64.encodeBase64String(privKey.bytes()));
    }

    @Test
    public void generateNativeKeyPair() throws Exception {
        HostBuilder hostBuilder = new HostBuilder();
        hostBuilder = hostBuilder.generateIdentity();
        // Type: Ed25519
        print("PrivKey: "+ new Gson().toJson(hostBuilder.getPrivateKey()));
        print("PubKey: "+ new Gson().toJson(hostBuilder.getPrivateKey().publicKey()));

        print("PrivKey Hex: "+ HexUtil.encodeHexString(hostBuilder.getPrivateKey().bytes())); // Not equiv format
        print("PrivKey Hex (raw): "+ HexUtil.encodeHexString(hostBuilder.getPrivateKey().raw())); // Not equiv format

        // IPFS uses base64 encoded priv keys
        String b64String = io.ipfs.multibase.binary.Base64.encodeBase64String(hostBuilder.getPrivateKey().raw());
        print("PrivKey (Base64): "+ b64String);

        // IPFS uses base58 encoded pubkey for peerid
        print("PeerId: "+ PeerId.fromPubKey(hostBuilder.getPrivateKey().publicKey()).toBase58());
        print("PeerId2: "+ hostBuilder.getPeerId().toBase58());

        byte[] decodedExPrivKey = io.ipfs.multibase.binary.Base64.decodeBase64(b64String);
        print("Re-encoded PrivKey (Base64): "+ io.ipfs.multibase.binary.Base64.encodeBase64String(decodedExPrivKey));
    }

    /* Gen keypair - NOT USED */
    @Test
    public void generateKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.genKeyPair();

        // extract the encoded private key, this is an unencrypted PKCS#8 private key
        byte[] encodedprivkey = keyPair.getPrivate().getEncoded();
        print("Encoded Key: "+ HexUtil.encodeHexString(encodedprivkey));
        print("PrivKey: "+ keyPair.getPrivate());

        String MYPBEALG = "PBEWithSHA1AndDESede";
        String password = "apass";

        int count = 20;// hash iteration count
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[8];
        random.nextBytes(salt);

        PBEParameterSpec pbeParamSpec = new PBEParameterSpec(salt, count);
        PBEKeySpec pbeKeySpec = new PBEKeySpec(password.toCharArray());
        SecretKeyFactory keyFac = SecretKeyFactory.getInstance(MYPBEALG);
        SecretKey pbeKey = keyFac.generateSecret(pbeKeySpec);

        Cipher pbeCipher = Cipher.getInstance(MYPBEALG);
        pbeCipher.init(Cipher.ENCRYPT_MODE, pbeKey, pbeParamSpec);

        byte[] ciphertext = pbeCipher.doFinal(encodedprivkey);

        AlgorithmParameters algparms = AlgorithmParameters.getInstance(MYPBEALG);
        algparms.init(pbeParamSpec);
        EncryptedPrivateKeyInfo encinfo = new EncryptedPrivateKeyInfo(algparms, ciphertext);
        byte[] encryptedPkcs8 = encinfo.getEncoded();
        print("Key: "+ HexUtil.encodeHexString(encryptedPkcs8));
    }

    public void print(String message) {
        System.out.println(message);
    }
}
