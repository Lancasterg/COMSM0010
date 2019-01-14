package io.openvidu.js.java;

/*
 * The original file this file was adapted from can be found at:
 * https://docs.aws.amazon.com/ses/latest/DeveloperGuide/send-using-sdk-java.html
 */

import java.io.IOException;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api-mail")
public class AwsMail {

    static final String FROM = "master.cloudchat@gmail.com";
    private final String SUBJECT = "CloudChat Email Invitation";
    static final String TEXTBODY = "This email was sent through Amazon SES "
            + "using the AWS SDK for Java. Hello world!";


    @RequestMapping(value = "/sendInvite", method = RequestMethod.POST)
    public ResponseEntity<Object> sendMail(@RequestBody String recipient) throws IOException, ParseException {
        JSONObject toSessionName = (JSONObject) new JSONParser().parse(recipient);
        String TO = (String) toSessionName.get("emailAddress");
        String sessionName = (String) toSessionName.get("sessionName");
        System.out.print(TO);

        String url = String.format("https://www.cloudchat.link/?data=%s", sessionName);

        final String HTMLBODY = "<h1>Cloudchat email invitation!</h1>"
                + "<p>This email was sent from cloudchat. "
                + "Someone would like to video chat with you! \n"
                + String.format("Follow <a href=\"%s\">this link</a> to start chatting.", url);
        try {
            AmazonSimpleEmailService client =
                    AmazonSimpleEmailServiceClientBuilder.standard()
                            .withRegion(Regions.EU_WEST_1).build();
            SendEmailRequest request = new SendEmailRequest()
                    .withDestination(
                            new Destination().withToAddresses(TO))
                    .withMessage(new Message()
                            .withBody(new Body()
                                    .withHtml(new Content()
                                            .withCharset("UTF-8").withData(HTMLBODY))
                                    .withText(new Content()
                                            .withCharset("UTF-8").withData(TEXTBODY)))
                            .withSubject(new Content()
                                    .withCharset("UTF-8").withData(SUBJECT)))
                    .withSource(FROM);
            client.sendEmail(request);
            System.out.println("Email sent!");
        } catch (Exception ex) {
            System.out.println("The email was not sent. Error message: "
                    + ex.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_GATEWAY);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
