import '../core/network/api_client.dart';
import '../models/product_models.dart';

class CatalogService {
  const CatalogService({required this.apiClient});

  final ApiClient apiClient;

  Future<List<CatalogProduct>> obtenerCatalogoProductos() async {
    try {
      final response = await apiClient.getJson('/productos');
      final outerData = response['data'];
      if (outerData is Map<String, dynamic>) {
        final innerList = outerData['data'];
        if (innerList is List && innerList.isNotEmpty) {
          return innerList
              .whereType<Map<String, dynamic>>()
              .map(CatalogProduct.fromJson)
              .toList();
        }
      }
    } on ApiException catch (e) {
      if (e.statusCode == 401) rethrow;
    } catch (_) {}

    final catalogResponse = await apiClient.getJson('/api/gestopago/catalogo');
    final data = catalogResponse['data'];
    if (data is List) {
      return data
          .whereType<Map<String, dynamic>>()
          .map(CatalogProduct.fromJson)
          .toList();
    }
    return const <CatalogProduct>[];
  }
}

