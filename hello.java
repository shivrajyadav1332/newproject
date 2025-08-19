import java.util.*;
import java.time.Year;

public class CarManagementApp {

    // --- Car entity ---
    static class Car {
        private final String id;      // immutable unique id (UUID)
        private String make;
        private String model;
        private int year;
        private String color;
        private double price;

        public Car(String make, String model, int year, String color, double price) {
            this.id = UUID.randomUUID().toString();
            this.make = make;
            this.model = model;
            this.year = year;
            this.color = color;
            this.price = price;
        }

        public String getId() { return id; }
        public String getMake() { return make; }
        public String getModel() { return model; }
        public int getYear() { return year; }
        public String getColor() { return color; }
        public double getPrice() { return price; }

        public void setMake(String make) { this.make = make; }
        public void setModel(String model) { this.model = model; }
        public void setYear(int year) { this.year = year; }
        public void setColor(String color) { this.color = color; }
        public void setPrice(double price) { this.price = price; }

        @Override
        public String toString() {
            return String.format(
                "ID: %s | %s %s | Year: %d | Color: %s | Price: %.2f",
                id, make, model, year, color, price
            );
        }
    }

    // --- Repository (in-memory) ---
    static class CarRepository {
        private final Map<String, Car> db = new LinkedHashMap<>();

        public Car save(Car car) {
            db.put(car.getId(), car);
            return car;
        }

        public Optional<Car> findById(String id) {
            return Optional.ofNullable(db.get(id));
        }

        public List<Car> findAll() {
            return new ArrayList<>(db.values());
        }

        public List<Car> search(String keyword) {
            String k = keyword.toLowerCase();
            List<Car> out = new ArrayList<>();
            for (Car c : db.values()) {
                if (c.getMake().toLowerCase().contains(k)
                    || c.getModel().toLowerCase().contains(k)
                    || c.getColor().toLowerCase().contains(k)
                    || c.getId().toLowerCase().contains(k)) {
                    out.add(c);
                }
            }
            return out;
        }

        public boolean delete(String id) {
            return db.remove(id) != null;
        }

        public void clear() {
            db.clear();
        }
    }

    // --- Service (validation + business rules) ---
    static class CarService {
        private final CarRepository repo;

        public CarService(CarRepository repo) {
            this.repo = repo;
        }

        public Car addCar(String make, String model, int year, String color, double price) {
            validate(make, model, year, color, price);
            return repo.save(new Car(cap(make), cap(model), year, cap(color), price));
        }

        public boolean updateCar(String id, String make, String model, Integer year, String color, Double price) {
            Optional<Car> opt = repo.findById(id);
            if (opt.isEmpty()) return false;
            Car c = opt.get();

            // Only update fields that are non-null
            if (make != null && !make.isBlank()) c.setMake(cap(make));
            if (model != null && !model.isBlank()) c.setModel(cap(model));
            if (year != null) {
                int y = year;
                validateYear(y);
                c.setYear(y);
            }
            if (color != null && !color.isBlank()) c.setColor(cap(color));
            if (price != null) {
                validatePrice(price);
                c.setPrice(price);
            }
            return true;
        }

        public List<Car> listAll() { return repo.findAll(); }
        public List<Car> search(String keyword) { return repo.search(keyword); }
        public boolean delete(String id) { return repo.delete(id); }

        // --- validation helpers ---
        private void validate(String make, String model, int year, String color, double price) {
            if (make == null || make.isBlank()) throw new IllegalArgumentException("Make is required.");
            if (model == null || model.isBlank()) throw new IllegalArgumentException("Model is required.");
            if (color == null || color.isBlank()) throw new IllegalArgumentException("Color is required.");
            validateYear(year);
            validatePrice(price);
        }

        private void validateYear(int year) {
            int current = Year.now().getValue();
            if (year < 1886 || year > current + 1) {
                throw new IllegalArgumentException("Year must be between 1886 and " + (current + 1));
            }
        }

        private void validatePrice(double price) {
            if (price < 0) throw new IllegalArgumentException("Price must be non-negative.");
        }

        private static String cap(String s) {
            s = s.trim();
            if (s.isEmpty()) return s;
            return s.substring(0,1).toUpperCase() + s.substring(1);
        }
    }

    // --- Console UI ---
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        CarService service = new CarService(new CarRepository());

        seed(service); // add some sample data

        while (true) {
            printMenu();
            System.out.print("Choose an option: ");
            String choice = sc.nextLine().trim();

            try {
                switch (choice) {
                    case "1" -> addCarFlow(sc, service);
                    case "2" -> listCarsFlow(service);
                    case "3" -> searchFlow(sc, service);
                    case "4" -> updateFlow(sc, service);
                    case "5" -> deleteFlow(sc, service);
                    case "6" -> {
                        System.out.println("Exiting. Bye!");
                        return;
                    }
                    default -> System.out.println("Invalid option. Try again.");
                }
            } catch (IllegalArgumentException ex) {
                System.out.println("Validation error: " + ex.getMessage());
            } catch (Exception ex) {
                System.out.println("Unexpected error: " + ex.getMessage());
            }

            System.out.println();
        }
    }

    private static void printMenu() {
        System.out.println("===== Car Management =====");
        System.out.println("1. Add Car");
        System.out.println("2. List All Cars");
        System.out.println("3. Search Cars");
        System.out.println("4. Update Car");
        System.out.println("5. Delete Car");
        System.out.println("6. Exit");
    }

    private static void addCarFlow(Scanner sc, CarService service) {
        System.out.print("Make: ");
        String make = sc.nextLine();
        System.out.print("Model: ");
        String model = sc.nextLine();
        System.out.print("Year: ");
        int year = Integer.parseInt(sc.nextLine().trim());
        System.out.print("Color: ");
        String color = sc.nextLine();
        System.out.print("Price: ");
        double price = Double.parseDouble(sc.nextLine().trim());

        Car c = service.addCar(make, model, year, color, price);
        System.out.println("Added:\n" + c);
    }

    private static void listCarsFlow(CarService service) {
        List<Car> cars = service.listAll();
        if (cars.isEmpty()) {
            System.out.println("No cars found.");
            return;
        }
        System.out.println("---- Cars (" + cars.size() + ") ----");
        for (Car c : cars) {
            System.out.println(c);
        }
    }

    private static void searchFlow(Scanner sc, CarService service) {
        System.out.print("Enter keyword (id/make/model/color): ");
        String k = sc.nextLine();
        List<Car> res = service.search(k);
        if (res.isEmpty()) {
            System.out.println("No matches.");
            return;
        }
        System.out.println("---- Results (" + res.size() + ") ----");
        for (Car c : res) {
            System.out.println(c);
        }
    }

    private static void updateFlow(Scanner sc, CarService service) {
        System.out.print("Enter Car ID to update: ");
        String id = sc.nextLine().trim();

        System.out.print("New make (leave blank to keep): ");
        String make = blankToNull(sc.nextLine());
        System.out.print("New model (leave blank to keep): ");
        String model = blankToNull(sc.nextLine());
        System.out.print("New year (leave blank to keep): ");
        String yearStr = sc.nextLine().trim();
        Integer year = yearStr.isBlank() ? null : Integer.parseInt(yearStr);
        System.out.print("New color (leave blank to keep): ");
        String color = blankToNull(sc.nextLine());
        System.out.print("New price (leave blank to keep): ");
        String priceStr = sc.nextLine().trim();
        Double price = priceStr.isBlank() ? null : Double.parseDouble(priceStr);

        boolean ok = service.updateCar(id, make, model, year, color, price);
        System.out.println(ok ? "Updated successfully." : "Car not found.");
    }

    private static void deleteFlow(Scanner sc, CarService service) {
        System.out.print("Enter Car ID to delete: ");
        String id = sc.nextLine().trim();
        boolean ok = service.delete(id);
        System.out.println(ok ? "Deleted." : "Car not found.");
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private static void seed(CarService service) {
        service.addCar("Toyota", "Corolla", 2020, "White", 15000);
        service.addCar("Honda", "Civic", 2019, "Blue", 14000);
        service.addCar("Tata", "Nexon", 2023, "Red", 1200000);
    }
}

