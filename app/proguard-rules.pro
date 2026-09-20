# Reguły R8 dla buildu release.
#
# Aplikacja nie używa serializacji ani refleksji po własnych klasach, więc nie
# potrzebuje dodatkowych reguł "keep":
#   - MainActivity jest wskazywana w AndroidManifest.xml, więc AGP dokłada dla niej
#     regułę keep automatycznie,
#   - modele (AuthUser, UserQrItem, ...) są (de)serializowane ręcznie przez JSONObject,
#   - callbacki modułów to zwykłe interfejsy wywoływane bezpośrednio,
#   - jedyne odbicie w kodzie to HttpURLConnection.method w SupabaseClient (metoda PATCH);
#     to klasa platformy Androida, której R8 nie obfusuje.
