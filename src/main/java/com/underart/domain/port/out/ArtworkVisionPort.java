package com.underart.domain.port.out;

import com.underart.domain.model.ArtworkDraft;
import com.underart.domain.model.AuctionCurrency;

public interface ArtworkVisionPort {

    ArtworkDraft analyze(byte[] image, String contentType, AuctionCurrency currency);
}
