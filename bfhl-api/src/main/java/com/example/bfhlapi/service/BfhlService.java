package com.example.bfhlapi.service;

import com.example.bfhlapi.dto.BfhlRequest;
import com.example.bfhlapi.dto.BfhlResponse;

public interface BfhlService {

    BfhlResponse process(BfhlRequest request, String requestId);
}
