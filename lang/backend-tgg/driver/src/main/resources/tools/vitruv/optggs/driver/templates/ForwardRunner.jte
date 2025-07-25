@import tools.vitruv.optggs.driver.Project
@param Project p
/**
* Generated by OpTGGs
*/
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.emoflon.neo.api.${p.packageName()}.API_Common;
import org.emoflon.neo.api.${p.packageName()}.API_Transform;
import org.emoflon.neo.api.${p.packageName()}.tgg.API_${p.name()}_FWD;
import org.emoflon.neo.api.${p.packageName()}.API_SourceMetamodel;
import org.emoflon.neo.emsl.eMSL.Model;
import org.emoflon.neo.cypher.models.NeoCoreBuilder;
import org.emoflon.neo.engine.modules.NeoGenerator;

import org.emoflon.neo.engine.modules.cleanup.RemoveTranslateAttributes;
import org.emoflon.neo.engine.modules.matchreprocessors.ParanoidNeoReprocessor;
import org.emoflon.neo.engine.modules.monitors.HeartBeatAndReportMonitor;
import org.emoflon.neo.engine.modules.ruleschedulers.AllRulesAllMatchesScheduler;
import org.emoflon.neo.engine.modules.startup.PrepareTranslateAttributes;
import org.emoflon.neo.engine.modules.terminationcondition.NoMoreMatchesTerminationCondition;
import org.emoflon.neo.engine.modules.updatepolicies.AnySingleMatchUpdatePolicy;
import org.emoflon.neo.engine.modules.valueGenerators.LoremIpsumStringValueGenerator;
import org.emoflon.neo.engine.modules.valueGenerators.ModelNameValueGenerator;
import java.util.List;

@SuppressWarnings("unused")
public class ${p.name()}ForwardRunner {
    protected static final Logger logger = Logger.getLogger(${p.name()}ForwardRunner.class);
    protected String srcModelName;
    protected String trgModelName;

    public ${p.name()}ForwardRunner(String srcModelName, String trgModelName) {
        this.srcModelName = srcModelName;
        this.trgModelName = trgModelName;
    }

    public void run(Model model) throws Exception {
        try (var builder = API_Common.createBuilder()) {
            new API_Transform(builder).exportMetamodelsFor${p.name()}();

            builder.clearDataBase();
            builder.exportEMSLEntityToNeo4j(model);
            var generator = createGenerator(builder);

            logger.info("Running generator...");
            generator.generate();
            logger.info("Generator terminated.");
        }
    }

    public NeoGenerator createGenerator(NeoCoreBuilder builder) {
        var gen = new NeoGenerator(//
            new API_${p.name()}_FWD(builder).getAllRulesFor${p.name()}_FWD(), //
            new PrepareTranslateAttributes(builder, srcModelName), //
            new NoMoreMatchesTerminationCondition(), //
            new AllRulesAllMatchesScheduler(), //
            new AnySingleMatchUpdatePolicy(), //
            new ParanoidNeoReprocessor(), //
            new RemoveTranslateAttributes(builder, srcModelName), //
            new HeartBeatAndReportMonitor(), //
            new ModelNameValueGenerator(srcModelName, trgModelName), //
            List.of(new LoremIpsumStringValueGenerator())
        );
        RegisterAttributeConstraints.register(gen);
        return gen;
    }
}
