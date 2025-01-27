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

import lombok.extern.slf4j.Slf4j;
import oap.ws.WsMethod;
import oap.ws.WsParam;

import java.util.List;

import static oap.http.server.nio.HttpServerExchange.HttpMethod.DELETE;
import static oap.ws.WsParam.From.PATH;

@Slf4j
public class AdminWS {
    private final OrganizationStorage organizationStorage;
    private final UserStorage userStorage;

    public AdminWS( OrganizationStorage organizationStorage, UserStorage userStorage ) {
        this.organizationStorage = organizationStorage;
        this.userStorage = userStorage;
    }

    @SuppressWarnings( "checkstyle:UnnecessaryParentheses" )
    @WsMethod( method = DELETE, path = "/organizations/{organizationId}" )
    public void deleteOrganization( @WsParam( from = PATH ) String organizationId ) {
        log.debug( "permanentlyDeleteOrganization {}", organizationId );

        userStorage
            .select()
            .filter( ud -> ud.accounts.containsKey( organizationId ) || ud.roles.containsKey( organizationId ) )
            .forEach( ud -> {
                if( ( ud.accounts.containsKey( organizationId ) && ud.accounts.size() == 1 )
                    || ( ud.roles.containsKey( organizationId ) && ud.roles.size() == 1 ) ) {
                    log.trace( "permanentlyDeleteOrganization#delete user {}", ud.user.getEmail() );
                    userStorage.permanentlyDelete( ud.user.getEmail() );
                } else {
                    log.trace( "permanentlyDeleteOrganization#update user {}", ud.user.getEmail() );
                    userStorage.update( ud.user.getEmail(), d -> {
                        d.accounts.remove( organizationId );
                        d.roles.remove( organizationId );
                        return d;
                    } );
                }
            } );

        organizationStorage.permanentlyDelete( organizationId );
    }

    @WsMethod( method = DELETE, path = "/all" )
    public void deleteAll() {
        List<String> users = userStorage.select()
            .filter( u -> !userStorage.defaultSystemAdminEmail.equals( u.user.email ) )
            .map( u -> u.user.email )
            .toList();

        for( String user : users ) {
            userStorage.permanentlyDelete( user );
        }

        List<String> organizations = organizationStorage.select()
            .filter( o -> !organizationStorage.defaultOrganizationId.equals( o.organization.id ) )
            .map( o -> o.organization.id )
            .toList();

        for( String organization : organizations ) {
            organizationStorage.permanentlyDelete( organization );
        }
    }

    @WsMethod( method = DELETE, path = "/users/{email}" )
    public void deleteUser( @WsParam( from = PATH ) String email ) {
        log.debug( "permanentlyDeleteUser {}", email );

        userStorage.permanentlyDelete( UserStorage.prepareEmail( email ) );
    }
}
