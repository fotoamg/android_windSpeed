/*
 * Copyright (c) 2018.  Inverness Park Corporation
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to
 * do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all copies or
 *  substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 * OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package hu.fotoamg.windspeed.nmea;

import hu.fotoamg.windspeed.nmea.Sentences.GGA;
import hu.fotoamg.windspeed.nmea.Sentences.GSA;
import hu.fotoamg.windspeed.nmea.Sentences.GST;
import hu.fotoamg.windspeed.nmea.Sentences.GSV;
import hu.fotoamg.windspeed.nmea.Sentences.HDT;
import hu.fotoamg.windspeed.nmea.Sentences.RMC;
import hu.fotoamg.windspeed.nmea.Sentences.VTG;

/**
 * ﻿NMEA handler contract, to be implemented by the application layer.
 *
 *﻿ Note: This is where you can add support for more NMEA data types.
 */

public interface INmeaHandler {
    void HandleGGA(GGA msg);
    void HandleGSA(GSA msg);
    void HandleGST(GST msg);
    void HandleGSV(GSV msg);
    void HandleHDT(HDT msg);
    void HandleRMC(RMC msg);
    void HandleVTG(VTG msg);
}
