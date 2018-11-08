package org.ohdsi.webapi.facets;

import org.springframework.stereotype.Component;

@Component
public class IdFilterProvider extends AbstractTextColumnFilterProvider {
    public static final String COLUMN_NAME = "Id";
    private static final String FIELD_NAME = "id";

    @Override
    public String getName() {
        return COLUMN_NAME;
    }

    @Override
    public String getField() {
        return FIELD_NAME;
    }
}
