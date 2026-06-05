package com.example.ragbackend.parser;

import com.example.ragbackend.common.exception.BusinessException;

public class UnsupportedDocumentTypeException extends BusinessException {

    private static final String ERROR_CODE = "UNSUPPORTED_DOCUMENT_TYPE";

    public UnsupportedDocumentTypeException(String fileType) {
        super(ERROR_CODE, "Unsupported document type: " + fileType);
    }
}
