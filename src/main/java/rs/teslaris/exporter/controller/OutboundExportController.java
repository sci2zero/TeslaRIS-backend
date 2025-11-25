package rs.teslaris.exporter.controller;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.TimeZone;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.model.oaipmh.common.OAIPMHResponse;
import rs.teslaris.core.model.oaipmh.common.Request;
import rs.teslaris.core.util.xmlutil.XMLUtil;
import rs.teslaris.exporter.service.interfaces.OutboundExportService;
import rs.teslaris.exporter.util.ExportDataFormat;
import rs.teslaris.exporter.util.OAIErrorFactory;
import rs.teslaris.importer.utility.oaipmh.OAIPMHParseUtility;

@Slf4j
@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
@Traceable
public class OutboundExportController {

    private final OutboundExportService outboundExportService;

    @Value("${export.base.url}")
    private String baseUrl;


    @GetMapping(value = "/{handlerName}", produces = "application/xml")
    public OAIPMHResponse handleOAIOpenAIRECRIS(@RequestParam(required = false) String verb,
                                                @RequestParam(required = false)
                                                String metadataPrefix,
                                                @RequestParam(required = false) String from,
                                                @RequestParam(required = false) String until,
                                                @RequestParam(required = false) String set,
                                                @RequestParam(required = false) String identifier,
                                                @RequestParam(required = false)
                                                String resumptionToken,
                                                @PathVariable String handlerName) {
        var response =
            performExport(handlerName, verb, metadataPrefix, from, until, set, identifier,
                resumptionToken);
        XMLUtil.cleanXmlObject(response);

        return response;
    }

    private OAIPMHResponse performExport(String handlerName,
                                         String verb,
                                         String metadataPrefix,
                                         String from,
                                         String until,
                                         String set,
                                         String identifier,
                                         String resumptionToken) {
        var response = new OAIPMHResponse();

        var utcDateTime = ZonedDateTime.now(TimeZone.getTimeZone("UTC").toZoneId());
        response.setResponseDate(utcDateTime.format(DateTimeFormatter.ISO_INSTANT));

        response.setRequest(
            new Request(Objects.nonNull(verb) ? verb : "", set, metadataPrefix,
                baseUrl + "/api/export/" + handlerName));

        if (Objects.isNull(verb)) {
            response.setError(OAIErrorFactory.constructNoVerbError());
            return response;
        }

        if (Objects.nonNull(metadataPrefix)) {
            metadataPrefix = metadataPrefix.toLowerCase();
        } else {
            metadataPrefix = ExportDataFormat.DUBLIN_CORE.getStringValue();
        }

        if (Objects.isNull(from)) {
            from = "1000-01-01";
        }

        if (Objects.isNull(until)) {
            until = "3000-12-31";
        }

        switch (verb) {
            case "Identify":
                response.setIdentify(outboundExportService.identifyHandler(handlerName));
                break;
            case "ListSets":
                response.setListSets(outboundExportService.listSetsForHandler(handlerName));
                break;
            case "ListMetadataFormats":
                response.setListMetadataFormats(
                    outboundExportService.listMetadataFormatsForHandler(handlerName));
                break;
            case "ListRecords", "ListIdentifiers":
                if (Objects.nonNull(resumptionToken)) {
                    if (!OAIPMHParseUtility.validateResumptionToken(resumptionToken)) {
                        response.setError(OAIErrorFactory.constructBadResumptionTokenError());
                        break;
                    }

                    OAIPMHParseUtility.ResumptionTokenData dataFromToken;
                    try {
                        dataFromToken = OAIPMHParseUtility.parseResumptionToken(resumptionToken);
                    } catch (IllegalArgumentException e) {
                        response.setError(OAIErrorFactory.constructBadResumptionTokenError());
                        break;
                    }

                    try {
                        response.setListRecords(
                            outboundExportService.listRequestedRecords(handlerName,
                                dataFromToken.format(),
                                dataFromToken.from(), dataFromToken.until(), dataFromToken.set(),
                                response, dataFromToken.page(), verb.equals("ListIdentifiers")));
                    } catch (Exception e) {
                        logError(e.getMessage(), verb);
                        response.setError(OAIErrorFactory.constructServiceUnavailableError());
                    }
                } else {
                    try {
                        response.setListRecords(
                            outboundExportService.listRequestedRecords(handlerName, metadataPrefix,
                                from, until, set, response, 0, verb.equals("ListIdentifiers")));
                    } catch (Exception e) {
                        logError(e.getMessage(), verb);
                        response.setError(OAIErrorFactory.constructServiceUnavailableError());
                    }
                }
                break;
            case "GetRecord":
                try {
                    response.setGetRecord(
                        outboundExportService.listRequestedRecord(handlerName, metadataPrefix,
                            identifier, response));
                } catch (Exception e) {
                    logError(e.getMessage(), verb);
                    response.setError(OAIErrorFactory.constructServiceUnavailableError());
                }
                break;
            default:
                response.setError(OAIErrorFactory.constructBadVerbError());
        }

        return response;
    }

    private void logError(String errorMessage, String verb) {
        log.error("Export error occurred. Verb: {}. Reason: {}", verb, errorMessage);
    }
}
