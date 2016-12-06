package main.java.servermanagement.util.mail;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import main.java.servermanagement.util.ConfigUtils;
import model.ConfigModel;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;

import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListLabelsResponse;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartBody;
import com.google.api.services.gmail.model.ModifyMessageRequest;

public class FetchingEmail {
  private static final String user = "me";

  public static void listLabels() throws IOException {
    Gmail service = GmailServiceBuilder.INSTANCE.getGmailService();
    ListLabelsResponse response = service.users().labels().list(user).execute();
    List<Label> labels = response.getLabels();
    for (Label label : labels) {
      System.out.println(label.toPrettyString());
    }
  }

  private Message markaAsReadAndArchieveTheEmail(Gmail service, String emailId) throws IOException {
    ModifyMessageRequest mods = new ModifyMessageRequest().setAddLabelIds(Arrays.asList("Label_2"))
        .setRemoveLabelIds(Arrays.asList("UNREAD", "INBOX"));

    return service.users().messages().modify(user, emailId, mods).execute();
  }

  public JSONObject checkForEmail() throws IOException {
    Gmail service = GmailServiceBuilder.INSTANCE.getGmailService();
    ConfigModel configModel = ConfigUtils.loadConfigJson();

    ListMessagesResponse messagesResponse = service.users().messages().list(user)
        .setQ("is:unread in:inbox from:" + configModel.mailSender).execute();
    List<Message> messages = messagesResponse.getMessages();
    List<String> messageIds = messages.stream().map(Message::getId).collect(Collectors.toList());
    Message msg;

    JSONObject jsonObject = new JSONObject();

    for (String messageId : messageIds) {
      msg = service.users().messages().get(user, messageId).setFormat("raw").execute();
      String messageContentRaw = msg.getSnippet();

      List<String> messageContent = Arrays.asList(messageContentRaw.split(","));

      for (String rawObject : messageContent) {
        try {
          rawObject = StringUtils.removeStart(rawObject, " ");
          rawObject = StringUtils.removeEnd(rawObject, " ");
          parseAndPutIntoJson(jsonObject, rawObject);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

      markaAsReadAndArchieveTheEmail(service, messageId);
    }

    return jsonObject;
  }

  @SuppressWarnings("unchecked")
  private void parseAndPutIntoJson(JSONObject jsonObject, String raw) throws Exception {
    String[] splitted = raw.split("\\s+");
    jsonObject.put(splitted[0], splitted[1]);
  }

  @SuppressWarnings("static-access")
  public void getAttachments(Gmail service, String userId, String messageId) throws IOException {
    Message message = service.users().messages().get(userId, messageId).execute();
    List<MessagePart> parts = message.getPayload().getParts();
    for (MessagePart part : parts) {
      if (part.getFilename() != null && part.getFilename().length() > 0) {
        String filename = part.getFilename();
        String attId = part.getBody().getAttachmentId();
        MessagePartBody attachPart = service.users().messages().attachments().get(userId, messageId, attId).execute();

        Base64 base64url = new Base64(true);
        byte[] fileByteArray = base64url.decodeBase64(attachPart.getData());
        FileOutputStream fileOutFile = new FileOutputStream("/home/aram/Pictures/" + filename);
        fileOutFile.write(fileByteArray);
        fileOutFile.close();
      }
    }
  }
}