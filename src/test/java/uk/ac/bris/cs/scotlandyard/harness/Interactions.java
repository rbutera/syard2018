package uk.ac.bris.cs.scotlandyard.harness;

import java.util.List;

import uk.ac.bris.cs.scotlandyard.harness.TestHarness.AssertionContext;

class Interactions {
	private Interactions() {}

	static <T> void assertEach(AssertionContext sra, List<Requirement<T>> rs, T t) {
		rs.forEach(v -> v.check(sra, t));
	}

}
