package io.openvidu.js.java;
/*
 *  Copyright 2013-2016 Amazon.com,
 *  Inc. or its affiliates. All Rights Reserved.
 *
 *  Licensed under the Amazon Software License (the "License").
 *  You may not use this file except in compliance with the
 *  License. A copy of the License is located at
 *
 *      http://aws.amazon.com/asl/
 *
 *  or in the "license" file accompanying this file. This file is
 *  distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *  CONDITIONS OF ANY KIND, express or implied. See the License
 *  for the specific language governing permissions and
 *  limitations under the License.
 *
 *  The original file this file was adapted from can be found at:
 *  https://github.com/aws-samples/aws-cognito-java-desktop-app/blob/master/src/main/java/com/amazonaws/sample/cognitoui/CognitoHelper.java
 *
 *  Modified by George Lancaster
 */


import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClientBuilder;
import com.amazonaws.services.cognitoidp.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;



public class AwsAuth {
    private String userPoolID;
    private String clientID;
    private String fedPoolID;
    private String region;


    AwsAuth(){

        Properties prop = new Properties();
        InputStream input = null;

        try {
            input = getClass().getClassLoader().getResourceAsStream("config.properties");

            // load a properties file
            prop.load(input);

            // Read the property values
            userPoolID = prop.getProperty("POOL_ID");
            clientID = prop.getProperty("CLIENTAPP_ID");
            fedPoolID = prop.getProperty("FED_POOL_ID");
            region = prop.getProperty("REGION");

        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Sign up the user to the user pool
     *
     * @param username    User name for the sign up
     * @param password    Password for the sign up
     * @param email       email used to sign up
     * @return whether the call was successful or not.
     */
    public boolean signUpUser(String username, String password, String email) {
        AnonymousAWSCredentials awsCreds = new AnonymousAWSCredentials();
        AWSCognitoIdentityProvider cognitoIdentityProvider = AWSCognitoIdentityProviderClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .withRegion(Regions.fromName(region))
                .build();

        SignUpRequest signUpRequest = new SignUpRequest();
        signUpRequest.setClientId(clientID);
        signUpRequest.setUsername(username);
        signUpRequest.setPassword(password);
        List<AttributeType> list = new ArrayList<>();

        AttributeType attributeType1 = new AttributeType();
        attributeType1.setName("email");
        attributeType1.setValue(email);
        list.add(attributeType1);

        signUpRequest.setUserAttributes(list);

        try {
            SignUpResult result = cognitoIdentityProvider.signUp(signUpRequest);
            System.out.println(result);
        } catch (Exception e) {
            System.out.println(e);
            System.out.println(e.getMessage());
            return false;
        }
        return true;
    }
    /**
     * Helper method to validate the user
     *
     * @param username represents the username in the cognito user pool
     * @param password represents the password in the cognito user pool
     * @return returns the JWT token after the validation
     */
    String ValidateUser(String username, String password) {
        AuthenticationHelper helper = new AuthenticationHelper(userPoolID, clientID, "");
        return helper.PerformSRPAuthentication(username, password);
    }

    /**
     * Start reset password procedure by sending reset code
     *
     * @param username user to be reset
     * @return returns code delivery details
     */
    String resetPassword(String username) {
        AnonymousAWSCredentials awsCreds = new AnonymousAWSCredentials();
        AWSCognitoIdentityProvider cognitoIdentityProvider = AWSCognitoIdentityProviderClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .withRegion(Regions.fromName(region))
                .build();

        ForgotPasswordRequest forgotPasswordRequest = new ForgotPasswordRequest();
        forgotPasswordRequest.setUsername(username);
        forgotPasswordRequest.setClientId(clientID);
        ForgotPasswordResult forgotPasswordResult = new ForgotPasswordResult();

        try {
            forgotPasswordResult = cognitoIdentityProvider.forgotPassword(forgotPasswordRequest);
        } catch (Exception e) {
            // handle exception here
        }
        return forgotPasswordResult.toString();
    }
}
