public class RegisterState {

	private Integer value;
	private Integer latestUsedAsDestBy;

	public RegisterState(Integer value, Integer latestUsedBy) {

		this.value = value;
		this.latestUsedAsDestBy = latestUsedBy;
	}

	public Integer getValue() {
		return value;
	}

	public void setValue(Integer value) {
		this.value = value;
	}

	public Integer getLatestUsedAsDestBy() {
		return latestUsedAsDestBy;
	}

	public void setLatestUsedAsDestBy(Integer latestUsedBy) {
		this.latestUsedAsDestBy = latestUsedBy;
	}

}
