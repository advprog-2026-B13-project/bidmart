package id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.converter;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.enums.PermissionValue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PermissionValueConverterTest {

    private final PermissionValueConverter converter = new PermissionValueConverter();

    @Test
    void convertToDatabaseColumn_shouldReturnPermissionName() {
        assertEquals("admin:all", converter.convertToDatabaseColumn(PermissionValue.ADMIN));
    }

    @Test
    void convertToDatabaseColumn_null_shouldReturnNull() {
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void convertToEntityAttribute_shouldReturnEnum() {
        assertEquals(PermissionValue.AUCTION_CREATE, converter.convertToEntityAttribute("auction:create"));
    }

    @Test
    void convertToEntityAttribute_null_shouldReturnNull() {
        assertNull(converter.convertToEntityAttribute(null));
    }

    @Test
    void convertToEntityAttribute_unknown_shouldThrow() {
        assertThrows(IllegalArgumentException.class,
                () -> converter.convertToEntityAttribute("unknown:perm"));
    }
}
