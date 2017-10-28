package co.pugo.apps.foodtruckfinder.billing;

/**
 * An interface that provides an access to BillingLibrary methods
 */
public interface BillingProvider {
  BillingManager getBillingManager();
  boolean isPremiumPurchased();
}
