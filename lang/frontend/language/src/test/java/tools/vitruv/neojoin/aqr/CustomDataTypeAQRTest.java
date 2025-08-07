package tools.vitruv.neojoin.aqr;

import org.junit.jupiter.api.Test;

import static tools.vitruv.neojoin.aqr.AQRAssertions.assertThat;
import static tools.vitruv.neojoin.aqr.AQRTargetClassAssertions.assertThat;

public class CustomDataTypeAQRTest extends AbstractAQRTest {

	@Test
	void implicitDataType() {
		var aqr = parse("""
			from Restaurant r create {
				dailyRevenue1 = r.dailyRevenue
				dailyRevenue2 := r.dailyRevenue
			}
			""");

		assertThat(aqr)
			.hasTargetClass(
				"Restaurant", rest -> {
					assertThat(rest)
						.sourceIs(lookup("restaurant", "Restaurant"))
						.hasNoCondition()
						.hasCopiedAttribute("dailyRevenue1", lookup("restaurant", "Restaurant", "dailyRevenue"))
						.hasCalculatedAttribute("dailyRevenue2", "EFloat")
						.hasNoMoreFeatures();
				}
			)
			.hasNoMoreTargetClasses();
	}

	@Test
	void implicitEnum() {
		var aqr = parse("""
			from Food f create {
				type1 = f.type
				type2 := f.type
			}
			""");

		assertThat(aqr)
			.hasTargetClass(
				"Food", rest -> {
					assertThat(rest)
						.sourceIs(lookup("restaurant", "Food"))
						.hasNoCondition()
						.hasCopiedAttribute("type1", lookup("restaurant", "Food", "type"))
						.hasCalculatedAttribute("type2", "FoodType")
						.hasNoMoreFeatures();
				}
			)
			.hasNoMoreTargetClasses();
	}

	@Test
	void explicitImportedEnum() {
		var aqr = parse("""
			from Food f create {
				type1: FoodType = f.type
				type2: rest.FoodType = f.type
				type3: FoodType := f.type
				type4: rest.FoodType := f.type
			}
			""");

		assertThat(aqr)
			.hasTargetClass(
				"Food", rest -> {
					assertThat(rest)
						.sourceIs(lookup("restaurant", "Food"))
						.hasNoCondition()
						.hasCopiedAttribute("type1", lookup("restaurant", "Food", "type"))
						.hasCopiedAttribute("type2", lookup("restaurant", "Food", "type"))
						.hasCalculatedAttribute("type3", "FoodType")
						.hasCalculatedAttribute("type4", "FoodType")
						.hasNoMoreFeatures();
				}
			)
			.hasNoMoreTargetClasses();
	}

	@Test
	void explicitImportedDataType() {
		var aqr = parse("""
			from Restaurant r create {
				dailyRevenue1: Money = r.dailyRevenue
				dailyRevenue2: Money := r.dailyRevenue
			}
			""");

		assertThat(aqr)
			.hasTargetClass(
				"Restaurant", rest -> {
					assertThat(rest)
						.sourceIs(lookup("restaurant", "Restaurant"))
						.hasNoCondition()
						.hasCopiedAttribute("dailyRevenue1", lookup("restaurant", "Restaurant", "dailyRevenue"))
						.hasCalculatedAttribute("dailyRevenue2", "Money")
						.hasNoMoreFeatures();
				}
			)
			.hasNoMoreTargetClasses();
	}

}
