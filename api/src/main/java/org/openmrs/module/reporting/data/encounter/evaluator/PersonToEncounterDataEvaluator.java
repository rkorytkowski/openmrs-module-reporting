/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.reporting.data.encounter.evaluator;

import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.annotation.Handler;
import org.openmrs.api.context.Context;
import org.openmrs.module.reporting.data.encounter.EncounterDataUtil;
import org.openmrs.module.reporting.data.encounter.EvaluatedEncounterData;
import org.openmrs.module.reporting.data.encounter.definition.EncounterDataDefinition;
import org.openmrs.module.reporting.data.encounter.definition.PersonToEncounterDataDefinition;
import org.openmrs.module.reporting.data.person.EvaluatedPersonData;
import org.openmrs.module.reporting.data.person.service.PersonDataService;
import org.openmrs.module.reporting.dataset.query.service.DataSetQueryService;
import org.openmrs.module.reporting.evaluation.EvaluationContext;
import org.openmrs.module.reporting.evaluation.EvaluationException;
import org.openmrs.module.reporting.evaluation.context.PersonEvaluationContext;
import org.openmrs.module.reporting.query.person.PersonIdSet;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Evaluates a PersonToEncounterDataDefinition to produce a EncounterData
 */
@Handler(supports=PersonToEncounterDataDefinition.class, order=50)
public class PersonToEncounterDataEvaluator implements EncounterDataEvaluator {

	/** 
	 * @see EncounterDataEvaluator#evaluate(EncounterDataDefinition, EvaluationContext)
	 * @should return person data by for each encounter in the passed context
	 */
    @Override
	public EvaluatedEncounterData evaluate(EncounterDataDefinition definition, EvaluationContext encounterEvaluationContext) throws EvaluationException {

        DataSetQueryService dqs = Context.getService(DataSetQueryService.class);
        EvaluatedEncounterData c = new EvaluatedEncounterData(definition, encounterEvaluationContext);

        // create a map of encounter ids -> patient ids (assumption is that person_id = patient_id)
        Set<Integer> encIds = EncounterDataUtil.getEncounterIdsForContext(encounterEvaluationContext, true);
        Map<Integer, Integer> convertedIds = dqs.convertData(Patient.class, "patientId", null, Encounter.class, "patient.patientId", encIds);

        // create a new (person) evaluation context using the retrieved ids
        PersonEvaluationContext personEvaluationContext = new PersonEvaluationContext();
        personEvaluationContext.setBasePersons(new PersonIdSet(new HashSet(convertedIds.values())));

        // evaluate the joined definition via this person context
        PersonToEncounterDataDefinition def = (PersonToEncounterDataDefinition) definition;
        EvaluatedPersonData pd = Context.getService(PersonDataService.class).evaluate(def.getJoinedDefinition(), personEvaluationContext);

        // now create the result set by mapping the results in the person data set to encounter ids
        for (Integer encId : encIds) {
            c.addData(encId, pd.getData().get(convertedIds.get(encId)));
        }
        return c;

	}
}
