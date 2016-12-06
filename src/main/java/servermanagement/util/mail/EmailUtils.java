package main.java.servermanagement.util.mail;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import main.java.servermanagement.util.ConfigUtils;
import model.ConfigModel;

import model.EmailCommand;
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

public class EmailUtils {
  private Gmail service = null;
  private static final String user = "me";

  public static void listLabels() throws IOException {
    Gmail service = GmailServiceBuilder.INSTANCE.getGmailService();
    ListLabelsResponse response = service.users().labels().list(user).execute();
    List<Label> labels = response.getLabels();
    for (Label label : labels) {
      System.out.println(label.toPrettyString());
    }
  }

  public EmailUtils() {
    try {
      service = GmailServiceBuilder.INSTANCE.getGmailService();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void sentEmail(String to, String subject, String bodyText) throws MessagingException, IOException {
    MimeMessage emailContent = createEmail(to, subject, bodyText);

    sendMessage(emailContent);
  }

  private MimeMessage createEmail(String to, String subject, String bodyText) throws MessagingException {
    Properties props = new Properties();
    Session session = Session.getDefaultInstance(props, null);

    MimeMessage email = new MimeMessage(session);

    email.setFrom(new InternetAddress(user));
    email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to));
    email.setSubject(subject);
    email.setText(bodyText);

    return email;
  }

  private Message sendMessage(MimeMessage emailContent) throws MessagingException,
      IOException {
    Gmail service = GmailServiceBuilder.INSTANCE.getGmailService();
    Message message = createMessageWithEmail(emailContent);

    message = service.users().messages().send(user, message).execute();

    return message;
  }

  private Message createMessageWithEmail(MimeMessage emailContent) throws MessagingException, IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    emailContent.writeTo(buffer);
    byte[] bytes = buffer.toByteArray();
    String encodedEmail = Base64.encodeBase64URLSafeString(bytes);
    Message message = new Message();
    message.setRaw(encodedEmail);
    return message;
  }

  private Message markaAsReadAndArchieveTheEmail(Gmail service, String emailId) throws IOException {
    ModifyMessageRequest mods = new ModifyMessageRequest().setAddLabelIds(Arrays.asList("Label_2"))
        .setRemoveLabelIds(Arrays.asList("UNREAD", "INBOX"));

    return service.users().messages().modify(user, emailId, mods).execute();
  }

  public List<EmailCommand> checkForEmail(ConfigModel configModel) throws IOException {
    List<EmailCommand> commands = new ArrayList<>();

    ListMessagesResponse messagesResponse = service.users().messages().list(user)
        .setQ("is:unread in:inbox from:" + configModel.mailSender).execute();
    List<Message> messages = messagesResponse.getMessages();

    JSONObject jsonObject = null;
    if(messages != null) {

      List<String> messageIds = messages.stream()
              .map(Message::getId)
              .collect(Collectors.toList());
      Message msg;

      jsonObject = new JSONObject();

      for (String messageId : messageIds) {
        msg = service.users()
                .messages()
                .get(user, messageId)
                .setFormat("raw")
                .execute();
        String messageContentRaw = msg.getSnippet();

        List<String> messageContent = Arrays.asList(messageContentRaw.split(","));

        for (String rawObject : messageContent) {
          try {
            rawObject = StringUtils.removeStart(rawObject, " ");
            rawObject = StringUtils.removeEnd(rawObject, " ");
            //parseAndPutIntoJson(jsonObject, rawObject);
            commands.add(EmailCommand.createFromRawValue(rawObject));
          } catch (Exception e) {
            e.printStackTrace();
          }
        }

        markaAsReadAndArchieveTheEmail(service, messageId);
      }
    }

    return commands;
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