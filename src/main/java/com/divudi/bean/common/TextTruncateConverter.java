package com.divudi.bean.common;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

@FacesConverter("textTruncateConverter")
public class TextTruncateConverter implements Converter {
    
    private static final int MAX_LENGTH = 100;
    
    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        return value;
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) {
        if (value == null) {
            return "";
        }
        
        String text = value.toString();
        if (text.length() > MAX_LENGTH) {
            return text.substring(0, MAX_LENGTH) + "...";
        }
        
        return text;
    }
}
