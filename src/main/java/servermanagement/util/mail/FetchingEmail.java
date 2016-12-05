package main.java.servermanagement.util.mail;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;
import com.google.api.client.util.store.FileDataStoreFactory;

import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.*;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import main.java.servermanagement.util.ConfigUtils;
import model.ConfigModel;
import org.json.simple.JSONObject;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FetchingEmail {
    public JSONObject checkForEmail() throws IOException {
        Gmail service = GmailServiceBuilder.INSTANCE.getGmailService();
        String user = "me";
        ConfigModel configModel = ConfigUtils.loadConfigJson();

        ListMessagesResponse messagesResponse = service.users().messages().list(user)
                .setQ("is:unread in:inbox from:" + configModel.mailSender).execute();
        List<Message> messages = messagesResponse.getMessages();
        List<String> messageIds = messages.stream().map(Message::getId).collect(Collectors.toList());
        Message msg;

        JSONObject jsonObject = new JSONObject();

        for (String messageId : messageIds) {
            msg = service.users().messages().get(user, messageId).setFormat("raw").execute();
            byte[] emailBytes = msg.decodeRaw();
            String messageContent = new String(emailBytes, "UTF-8");

            List<String> wholeMail = Arrays.asList(messageContent.split("\\r?\\n"));

            List<String> content = wholeMail.subList(wholeMail.indexOf("***BEGIN***") + 1, wholeMail.indexOf("***END***"));


            for (String rawObject : content) {
                try {
                    parseAndPutIntoJson(jsonObject, rawObject);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return jsonObject;
    }

    private void parseAndPutIntoJson(JSONObject jsonObject, String raw) throws Exception {
        String[] splitted = raw.split("\\s+");
        jsonObject.put(splitted[0], splitted[1]);
    }

    /**
     * Creates an authorized Credential object.
     * @return an authorized Credential object.
     * @throws IOException
     */
    /*public Credential authorize() throws IOException {
        // Load client secrets.
        InputStream in =
                FetchingEmail.class.getResourceAsStream("/client_secret_gmail-api.json");
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(
                        HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                        .setDataStoreFactory(DATA_STORE_FACTORY)
                        .setAccessType("offline")
                        .build();
        Credential credential = new AuthorizationCodeInstalledApp(
                flow, new LocalServerReceiver()).authorize("user");
        System.out.println(
                "Credentials saved to " + DATA_STORE_DIR.getAbsolutePath());
        return credential;
    }*/

    /**
     * Build and return an authorized Gmail client service.
     * @return an authorized Gmail client service
     * @throws IOException
     */
    /*public Gmail getGmailService() throws IOException {
        Credential credential = authorize();
        return new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }*/

    public void getAttachments(Gmail service, String userId, String messageId)
            throws IOException {
        Message message = service.users().messages().get(userId, messageId).execute();
        List<MessagePart> parts = message.getPayload().getParts();
        for (MessagePart part : parts) {
            if (part.getFilename() != null && part.getFilename().length() > 0) {
                String filename = part.getFilename();
                String attId = part.getBody().getAttachmentId();
                MessagePartBody attachPart = service.users().messages().attachments().get(userId, messageId, attId).execute();

                Base64 base64url = new Base64(true);
                byte[] fileByteArray = base64url.decodeBase64(attachPart.getData());
                FileOutputStream fileOutFile =
                        new FileOutputStream("/home/aram/Pictures/" + filename);
                fileOutFile.write(fileByteArray);
                fileOutFile.close();
            }
        }
    }
}