package com.solar.Quotation.util;

import com.solar.Quotation.dto.responsedto.ReportRowResponseDto;

import java.util.List;

public final class PdfExportUtil {

    private PdfExportUtil() { }

    public static byte[] quotations(List<ReportRowResponseDto.Quotation> rows) {
        return new byte[0];   // TODO: implement with OpenPDF / iText
    }

    public static byte[] gst(List<ReportRowResponseDto.Gst> rows) {
        return new byte[0];   // TODO: implement with OpenPDF / iText
    }

    public static byte[] referral(List<ReportRowResponseDto.ReferralIncome> rows) {
        return new byte[0];   // TODO: implement with OpenPDF / iText
    }
}