package com.gis.logistics.infra.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.Point;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class GeoJacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jtsGeometrySerializer() {
        return builder -> {
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            builder.modules(new JavaTimeModule(),
                    new SimpleModule().addSerializer(Geometry.class, new GeometryGeoJsonSerializer()));
        };
    }

    static class GeometryGeoJsonSerializer extends JsonSerializer<Geometry> {
        @Override
        public void serialize(Geometry g, JsonGenerator gen, SerializerProvider sp) throws IOException {
            if (g == null) {
                gen.writeNull();
                return;
            }
            gen.writeStartObject();
            gen.writeFieldName("type");
            gen.writeString(g.getGeometryType());
            gen.writeFieldName("coordinates");
            writeCoordinates(g, gen);
            gen.writeEndObject();
        }

        private void writeCoordinates(Geometry g, JsonGenerator gen) throws IOException {
            if (g instanceof Point p) {
                gen.writeStartArray();
                gen.writeNumber(p.getX());
                gen.writeNumber(p.getY());
                gen.writeEndArray();
            } else if (g instanceof LineString ls) {
                gen.writeStartArray();
                for (int i = 0; i < ls.getNumPoints(); i++) {
                    gen.writeStartArray();
                    gen.writeNumber(ls.getCoordinateN(i).x);
                    gen.writeNumber(ls.getCoordinateN(i).y);
                    gen.writeEndArray();
                }
                gen.writeEndArray();
            } else if (g instanceof MultiPoint mp) {
                gen.writeStartArray();
                for (int i = 0; i < mp.getNumGeometries(); i++) {
                    writeCoordinates(mp.getGeometryN(i), gen);
                }
                gen.writeEndArray();
            } else {
                gen.writeString(g.toText());
            }
        }
    }
}