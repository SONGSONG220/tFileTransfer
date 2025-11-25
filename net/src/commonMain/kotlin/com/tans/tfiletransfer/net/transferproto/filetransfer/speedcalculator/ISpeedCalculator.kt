package com.tans.tfiletransfer.net.transferproto.filetransfer.speedcalculator

import kotlinx.coroutines.flow.Flow

interface ISpeedCalculator {
    fun speed(): Flow<Speed>
}