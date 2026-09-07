package com.underart.domain.port.in;

import com.underart.domain.model.Auction;

import java.util.List;

public interface ListAuctionsUseCase {

    List<Auction> listActive();

    Auction findById(Long id);
}
