package id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.converter;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.enums.PermissionValue;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PermissionValueConverter implements AttributeConverter<PermissionValue, String> {
    @Override
    public String convertToDatabaseColumn(PermissionValue perms) {
        if (perms == null) {
            return null;
        }

        return perms.getPermissionName();
    }

    @Override
    public PermissionValue convertToEntityAttribute(String permName) {
        if (permName == null) {
            return null;
        }

        return PermissionValue.fromName(permName);
    }
}
