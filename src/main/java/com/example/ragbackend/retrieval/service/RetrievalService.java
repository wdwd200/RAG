package com.example.ragbackend.retrieval.service;

import com.example.ragbackend.retrieval.dto.RetrieveRequest;
import com.example.ragbackend.retrieval.dto.RetrieveResponse;

public interface RetrievalService {

    RetrieveResponse retrieve(RetrieveRequest request);
}
