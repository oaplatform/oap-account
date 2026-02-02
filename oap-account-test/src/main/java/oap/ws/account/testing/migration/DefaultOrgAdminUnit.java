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

package oap.ws.account.testing.migration;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import oap.id.Identifier;
import oap.id.StringIdentifierBuilder;
import oap.ws.account.User;
import org.bson.Document;
import org.joda.time.DateTimeUtils;

import java.util.Map;

import static oap.id.Identifier.Option.COMPACT;

@ChangeUnit( id = "DefaultOrgAdminUnit", order = "1", systemVersion = "1" )
public class DefaultOrgAdminUnit {
    public static final String ORGADMIN_EMAIL = "orgadmin@admin.com";
    public static final String ORGADMIN_ID = Identifier.generate( ORGADMIN_EMAIL, StringIdentifierBuilder.DEFAULT_ID_SIZE, _ -> false, 0, COMPACT );
    public static final String ORGADMIN_PASSWORD = "Xenoss123";

    public static final String SYSTEMADMIN_EMAIL = "systemadmin@admin.com";
    public static final String SYSTEMADMIN_ID = Identifier.generate( SYSTEMADMIN_EMAIL, StringIdentifierBuilder.DEFAULT_ID_SIZE, _ -> false, 0, COMPACT );
    public static final String SYSTEMADMIN_PASSWORD = "Xenoss123";

    @Execution
    public void execution( MongoDatabase mongoDatabase ) {
        mongoDatabase.getCollection( "users" )
            .replaceOne( Filters.eq( "_id", ORGADMIN_ID ),
                new Document( Map.of(
                    "object", new Document( Map.of(
                        "user", new Document( Map.of(
                            "accessKey", "HMWDDRMHNGKU",
                            "firstName", "Johnny",
                            "lastName", "Walker",
                            "email", ORGADMIN_EMAIL,
                            "password", User.encrypt( ORGADMIN_PASSWORD ),
                            "confirmed", true,
                            "defaultOrganization", "DFLT",
                            "apiKey", "pz7r93Hh8ssbcV1Qhxsopej18ng2Q"
                        ) ),
                        "roles", new Document( Map.of( "DFLT", "ORGANIZATION_ADMIN" ) )
                    ) ),
                    "object:type", "user",
                    "modified", DateTimeUtils.currentTimeMillis()
                ) ), new ReplaceOptions().upsert( true ) );

        mongoDatabase.getCollection( "users" )
            .replaceOne( Filters.eq( "_id", SYSTEMADMIN_ID ),
                new Document( Map.of(
                    "object", new Document( Map.of(
                        "user", new Document( Map.of(
                            "firstName", "System",
                            "lastName", "Admin",
                            "email", SYSTEMADMIN_EMAIL,
                            "password", User.encrypt( SYSTEMADMIN_PASSWORD ),
                            "confirmed", true,
                            "defaultOrganization", "SYSTEM",
                            "apiKey", "qwfqwrqfdsgrwqewgreh4t2wrge43K"
                        ) ),
                        "roles", new Document( Map.of( "DFLT", "ORGANIZATION_ADMIN", "SYSTEM", "ADMIN" ) )
                    ) ),
                    "object:type", "user",
                    "modified", DateTimeUtils.currentTimeMillis()
                ) ), new ReplaceOptions().upsert( true ) );
    }

    @RollbackExecution
    public void rollback( MongoDatabase mongoDatabase ) {

    }
}
