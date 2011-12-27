import db
import re
import web
import view, config
from view import render

urls = (
    '/', 'index',
    '/show', 'show_entity'
)

class index:
    def GET(self):
        return render.base(view.listing())

class show_entity:
    def GET(self):
        entity = None
        entity_list = {}
        user_data = web.input()
        if user_data.has_key('entity'):
          entity = user_data.get('entity')
          k = {}
          k['what'] = 'entity2'
          k['where'] = 'entity1 = "%s"' % entity
          k['order'] = 'rank desc'
          k['limit'] = 10
          entities = db.entity_listing(**k)
          entity_list['entity'] = entity
          entity_list['entities'] = entities
        return render.base(view.listing(), entity_list = entity_list)

if __name__ == "__main__":
    app = web.application(urls, globals())
    app.internalerror = web.debugerror
    app.run()
