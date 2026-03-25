package org.ic4j.sample.mcp;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.ic4j.types.Principal;

public class SampleComplexPojo {
	public Optional<SampleEntryPojo> maybeEntry;
	public List<SampleEntryPojo> entries;
	public Map<String, String> labels;
	public Principal owner;
	public BigDecimal score;
	public Status status;

	public enum Status {
		ACTIVE,
		PAUSED,
		DISABLED
	}
}