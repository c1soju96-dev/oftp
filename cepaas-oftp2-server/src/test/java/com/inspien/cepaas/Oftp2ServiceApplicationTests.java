// package com.inspien.cepaas;

// import com.inspien.cepaas.server.OftpServerManager;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.neociclo.odetteftp.OdetteFtpClient;
// import org.neociclo.odetteftp.oftplet.OdetteFtpSession;
// import org.neociclo.odetteftp.protocol.DefaultVirtualFile;
// import org.neociclo.odetteftp.protocol.VirtualFile;
// import org.neociclo.odetteftp.support.OdetteFtpConfiguration;
// import org.neociclo.odetteftp.util.OftpHelper;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.test.context.TestPropertySource;

// import java.io.File;
// import java.io.FileInputStream;
// import java.io.FileOutputStream;

// import static org.junit.jupiter.api.Assertions.assertTrue;

// @SpringBootTest
// @TestPropertySource(locations = "classpath:application-test.properties")
// class OftpIntegrationTest {

//     @Autowired
//     private OftpServerManager serverManager;

//     private OdetteFtpClient client;

//     @BeforeEach
//     void setUp() throws Exception {
//         serverManager.startServer();
//         client = new OdetteFtpClient();
//         OdetteFtpConfiguration config = new OdetteFtpConfiguration();
//         config.setVirtualFileCompressionEnabled(true); // 예: 파일 압축 설정
//         client.configure(config);
//     }

//     @Test
//     void testFileTransfer() throws Exception {
//         // 테스트용 파일 생성
//         File testFile = new File("src/test/resources/test-file.txt");
//         try (FileOutputStream fos = new FileOutputStream(testFile)) {
//             fos.write("This is a test file.".getBytes());
//         }

//         // 서버에 전송할 VirtualFile 생성
//         VirtualFile vf = new DefaultVirtualFile(testFile, "test-file.txt");

//         // 클라이언트 설정
//         OdetteFtpSession session = OftpHelper.openClientSession(client, "localhost", 3305);

//         // 파일 전송
//         client.send(vf);

//         // 서버에서 파일 수신 여부 확인
//         File receivedFile = new File(serverManager.getServerDir(), "test-file.txt");
//         assertTrue(receivedFile.exists(), "File should be received by the server.");

//         // 서버 파일 내용 검증
//         try (FileInputStream fis = new FileInputStream(receivedFile)) {
//             byte[] content = fis.readAllBytes();
//             String receivedContent = new String(content);
//             assertTrue(receivedContent.contains("This is a test file."), "File content should match the original.");
//         }

//         // 클라이언트 세션 종료
//         session.close();
//     }
// }