/*
 * Copyright (c) Xenoss
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package oap.ws.account;


import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import oap.json.ext.Ext;
import oap.json.properties.PropertiesDeserializer;
import oap.ws.openapi.OpenApiDescription;

import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

@ToString
@EqualsAndHashCode
public class Account implements Serializable {
    public static final String SCHEMA = "/oap/ws/account/account.schema.conf";
    @Serial
    private static final long serialVersionUID = -1598345391160039855L;
    @JsonIgnore
    private final LinkedHashMap<String, Object> properties = new LinkedHashMap<>();
    @OpenApiDescription( "Id of the account" )
    public String id;
    @OpenApiDescription( "Name of the account" )
    public String name;
    @OpenApiDescription( "Description of the account" )
    public String description;
    @OpenApiDescription( "Additional extension data for the account" )
    public Ext ext;

    public Account() {
    }

    public Account( String name ) {
        this.name = name;
    }

    public Account( String id, String name ) {
        this.id = id;
        this.name = name;
    }

    @SuppressWarnings( "unchecked" )
    public <E extends Ext> E ext() {
        return ( E ) ext;
    }

    @JsonAnySetter
    @JsonDeserialize( contentUsing = PropertiesDeserializer.class )
    public void putProperty( String name, Object value ) {
        properties.put( name, value );
    }

    @JsonAnyGetter
    @OpenApiDescription( "Additional dynamic properties of the account" )
    public Map<String, Object> getProperties() {
        return properties;
    }

    @SuppressWarnings( "unchecked" )
    public <T> T getProperty( String property ) {
        return ( T ) properties.get( property );
    }
}
