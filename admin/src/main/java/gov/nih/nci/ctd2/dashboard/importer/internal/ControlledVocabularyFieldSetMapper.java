package gov.nih.nci.ctd2.dashboard.importer.internal;

import gov.nih.nci.ctd2.dashboard.model.SubjectRole;
import gov.nih.nci.ctd2.dashboard.model.ObservedSubjectRole;
import gov.nih.nci.ctd2.dashboard.model.EvidenceRole;
import gov.nih.nci.ctd2.dashboard.model.ObservedEvidenceRole;
import gov.nih.nci.ctd2.dashboard.model.ObservationTemplate;
import gov.nih.nci.ctd2.dashboard.model.DashboardFactory;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindException;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.HashSet;

@Component("controlledVocabularyMapper")
public class ControlledVocabularyFieldSetMapper implements FieldSetMapper<ControlledVocabulary> {

	private static final String TEMPLATE_NAME = "template_name";
	private static final String TEMPLATE_DESCRIPTION = "template_description";
	private static final String TEMPLATE_TIER = "template_tier";
	private static final String COLUMN_NAME = "column_name";
	private static final String SUBJECT = "subject";
	private static final String EVIDENCE = "evidence";
	private static final String ROLE = "role";
	private static final String DESCRIPTION = "description";
	private static final String MIME_TYPE = "mime_type";
	private static final String NUMERIC_UNITS = "numeric_units";
	private static final String URL_TEMPLATE = "url_template";

    @Autowired
    private DashboardFactory dashboardFactory;

	// cache for fast lookup and prevention of duplicate role records
    private HashMap<String, SubjectRole> subjectRoleCache = new HashMap<String, SubjectRole>();
    private HashMap<String, EvidenceRole> evidenceRoleCache = new HashMap<String, EvidenceRole>();
    private HashMap<String, ObservationTemplate> observationTemplateCache = new HashMap<String, ObservationTemplate>();

	public ControlledVocabulary mapFieldSet(FieldSet fieldSet) throws BindException {

		String templateName = fieldSet.readString(TEMPLATE_NAME);
		ObservationTemplate observationTemplate = observationTemplateCache.get(templateName);
		if (observationTemplate == null) {
			observationTemplate = dashboardFactory.create(ObservationTemplate.class);
			observationTemplate.setDisplayName(templateName);
			observationTemplate.setDescription(fieldSet.readString(TEMPLATE_DESCRIPTION));
			observationTemplate.setTier(fieldSet.readInt(TEMPLATE_TIER));
			observationTemplateCache.put(templateName, observationTemplate);
		}

		String subject = fieldSet.readString(SUBJECT);
		String evidence = fieldSet.readString(EVIDENCE);
		if (subject.length() > 0) {
			SubjectRole subjectRole = subjectRoleCache.get(subject);
			if (subjectRole == null) {
				subjectRole = dashboardFactory.create(SubjectRole.class);
				subjectRole.setDisplayName(fieldSet.readString(ROLE));
				subjectRoleCache.put(subject, subjectRole);
			}
			ObservedSubjectRole observedSubjectRole = dashboardFactory.create(ObservedSubjectRole.class);
			observedSubjectRole.setSubjectRole(subjectRole);
			observedSubjectRole.setColumnName(fieldSet.readString(COLUMN_NAME));
			observedSubjectRole.setDescription(fieldSet.readString(DESCRIPTION));
			observedSubjectRole.setObservationTemplate(observationTemplate);
			return new ControlledVocabulary(observationTemplate, subjectRole, observedSubjectRole);
		}
		else if (evidence.length() > 0) {
			EvidenceRole evidenceRole = evidenceRoleCache.get(evidence);
			if (evidenceRole == null) {
				evidenceRole = dashboardFactory.create(EvidenceRole.class);
				evidenceRole.setDisplayName(fieldSet.readString(ROLE));
				evidenceRoleCache.put(evidence, evidenceRole);
			}
			ObservedEvidenceRole observedEvidenceRole = dashboardFactory.create(ObservedEvidenceRole.class);
			observedEvidenceRole.setEvidenceRole(evidenceRole);
			observedEvidenceRole.setColumnName(fieldSet.readString(COLUMN_NAME));
			observedEvidenceRole.setDescription(fieldSet.readString(DESCRIPTION));
			observedEvidenceRole.setObservationTemplate(observationTemplate);
			observedEvidenceRole.setType(getObservedEvidenceRoleType(fieldSet.readString(MIME_TYPE),
																	 fieldSet.readString(NUMERIC_UNITS),
																	 fieldSet.readString(URL_TEMPLATE)));
			return new ControlledVocabulary(observationTemplate, evidenceRole, observedEvidenceRole);
		}
		return new ControlledVocabulary(null, null, null);
	}
	
	private String getObservedEvidenceRoleType(String mimeType, String numericUnits, String urlTemplate) {
		if (mimeType.length() > 0) return mimeType;
		if (numericUnits.length() > 0) return numericUnits;
		if (urlTemplate.length() > 0) return urlTemplate;
		return "";
	}
}