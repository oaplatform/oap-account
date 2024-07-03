/*
 * The MIT License (MIT)
 *
 * Copyright (c) Open Application Platform Authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package oap.ws.account;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.http.Client;
import oap.http.Http;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.Optional;

@Slf4j
public class GoogleProvider implements OauthProviderService {
    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    private static final GsonFactory GSON_FACTORY = new GsonFactory();
    private final String clientId;

    public GoogleProvider( String clientId ) {
        this.clientId = clientId;
    }

    /**
     * @param accessToken - jwt or access token
     * @return
     */
    public Optional<TokenInfo> getTokenInfo( String accessToken ) {
        if( StringUtils.split( "." ).length > 2 ) {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder( HTTP_TRANSPORT, GSON_FACTORY )
                .setAudience( Collections.singletonList( clientId ) )
                .build();

            try {
                GoogleIdToken idToken = verifier.verify( accessToken );
                if( idToken != null ) {
                    GoogleIdToken.Payload payload = idToken.getPayload();
                    return Optional.of( new TokenInfo( payload.getEmail(), ( String ) payload.get( "given_name" ), ( String ) payload.get( "family_name" ) ) );
                }
                return Optional.empty();
            } catch( Exception e ) {
                log.error( "Failed to extract user from google token", e );
                throw Throwables.propagate( e );
            }
        } else {
            try {
                Client.Response response = Client.DEFAULT.get( "https://www.googleapis.com/oauth2/v1/tokeninfo?access_token=" + accessToken );
                Preconditions.checkArgument( response.code == Http.StatusCode.OK );
                Optional<TokenInfoResponse> info = response.unmarshal( TokenInfoResponse.class );

                return info.map( i -> new TokenInfo( i.email, i.given_name, i.family_name ) );
            } catch( Exception e ) {
                log.error( "Failed to extract user from google token", e );
                throw Throwables.propagate( e );
            }
        }
    }

    @ToString
    public static class TokenInfoResponse {
        public String email;
        public String scope;
        @SuppressWarnings( "checkstyle:MemberName" )
        public String given_name;
        @SuppressWarnings( "checkstyle:MemberName" )
        public String family_name;
    }
}
