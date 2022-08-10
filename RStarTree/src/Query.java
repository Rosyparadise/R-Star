public class Query {
    protected static void invalidArgs(String desc) {
        switch (desc) {
            case "negative" -> System.out.println("Invalid arguments. Coordinates should only be positive.");
            case "overlap" ->
                    System.out.println("Invalid arguments. Coordinates between the same axis shouldn't overlap.");
            case "type" -> System.out.println("Invalid arguments. Coordinates should be Double/Float or Integers");
        }
    }
}
