package gov.nih.nci.ctd2.dashboard.importer.internal;

import gov.nih.nci.ctd2.dashboard.model.Xref;
import gov.nih.nci.ctd2.dashboard.model.Gene;
import gov.nih.nci.ctd2.dashboard.model.Synonym;
import gov.nih.nci.ctd2.dashboard.model.Organism;
import gov.nih.nci.ctd2.dashboard.model.DashboardFactory;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindException;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

@Component("geneDataMapper")
public class GeneDataFieldSetMapper implements FieldSetMapper<GeneData> {

	public static final String NCBI_GENE_DATABASE = "NCBI_GENE";

    @Autowired
    private DashboardFactory dashboardFactory;

    private HashMap<String, Organism> organismMap = new HashMap<String, Organism>();

	public GeneData mapFieldSet(FieldSet fieldSet) throws BindException {
        Gene gene = dashboardFactory.create(Gene.class);
		String entrezGeneId = fieldSet.readString(1);
        gene.setEntrezGeneId(entrezGeneId);
        gene.setDisplayName(fieldSet.readString(2));
		// create xref back to ncbik
		Xref xref = dashboardFactory.create(Xref.class);
		xref.setDatabaseId(entrezGeneId);
		xref.setDatabaseName(NCBI_GENE_DATABASE);
		gene.getXrefs().add(xref);
		for (String synonymName : fieldSet.readString(4).split("\\|")) {
			Synonym synonym = dashboardFactory.create(Synonym.class);
			synonym.setDisplayName(synonymName);
			gene.getSynonyms().add(synonym);
		}
		// hgnc parsing
		for (String dbXrefs : fieldSet.readString(5).split("\\|")) {
			String[] parts = dbXrefs.split("\\:");
			if (parts[0].equals("HGNC")) {
				gene.setHGNCId(parts[1]);
				break;
			}
		}

		boolean saveOrganism = false;
        String taxonomyId = fieldSet.readString(0);
        Organism organism = organismMap.get(taxonomyId);
        if (organism == null) {
            organism = dashboardFactory.create(Organism.class);
            organism.setTaxonomyId(taxonomyId);
            organismMap.put(taxonomyId, organism);
			saveOrganism = true;
        }

        gene.setOrganism(organism);

        return new GeneData(gene, organism, saveOrganism);
	}
}
