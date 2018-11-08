package org.ohdsi.webapi.ircalc.converter;

import org.ohdsi.webapi.converter.BaseConversionServiceAwareConverter;
import org.ohdsi.webapi.ircalc.IncidenceRateAnalysis;
import org.ohdsi.webapi.service.IRAnalysisService;
import org.ohdsi.webapi.util.UserUtils;
import org.springframework.stereotype.Component;

@Component
public class IrEntityToDtoConverter extends BaseConversionServiceAwareConverter<IncidenceRateAnalysis, IRAnalysisService.IRAnalysisListItem> {
    @Override
    protected IRAnalysisService.IRAnalysisListItem createResultObject() {
        return new IRAnalysisService.IRAnalysisListItem();
    }

    @Override
    public IRAnalysisService.IRAnalysisListItem convert(IncidenceRateAnalysis p) {
        final IRAnalysisService.IRAnalysisListItem item = createResultObject();
        item.id = p.getId();
        item.name = p.getName();
        item.description = p.getDescription();
        item.createdBy = UserUtils.nullSafeLogin(p.getCreatedBy());
        item.createdDate = p.getCreatedDate();
        item.modifiedBy = UserUtils.nullSafeLogin(p.getModifiedBy());
        item.modifiedDate = p.getModifiedDate();
        return item;
    }
}
