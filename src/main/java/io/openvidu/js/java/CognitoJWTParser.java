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
 *  https://github.com/aws-samples/aws-cognito-java-desktop-app/blob/master/src/main/java/com/amazonaws/sample/cognitoui/CognitoJWTParser.java
 *
 */


import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.json.JSONObject;

import java.security.InvalidParameterException;

/**
 * Utility class for all operations on JWT.
 */
public class CognitoJWTParser {
    private static final int PAYLOAD = 1;
    private static final int JWT_PARTS = 3;


    /**
     * Returns payload of a JWT as a JSON object.
     *
     * @param jwt REQUIRED: valid JSON Web Token as String.
     * @return payload as a JSONObject.
     */
    static JSONObject getPayload(String jwt) {
        try {
            validateJWT(jwt);
            Base64.Decoder dec = Base64.getDecoder();
            final String payload = jwt.split("\\.")[PAYLOAD];
            final byte[] sectionDecoded = dec.decode(payload);
            final String jwtSection = new String(sectionDecoded, StandardCharsets.UTF_8);
            return new JSONObject(jwtSection);
        } catch (final Exception e) {
            throw new InvalidParameterException("error in parsing JSON");
        }
    }

    /**
     * Checks if {@code JWT} is a valid JSON Web Token.
     *
     * @param jwt REQUIRED: The JWT as a {@link String}.
     */
    static void validateJWT(String jwt) {
        // Check if the the JWT has the three parts
        final String[] jwtParts = jwt.split("\\.");
        if (jwtParts.length != JWT_PARTS) {
            throw new InvalidParameterException("not a JSON Web Token");
        }
    }
}