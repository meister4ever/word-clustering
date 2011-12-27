import web
import db
import config

t_globals = dict(
  datestr=web.datestr,
)
render = web.template.render('templates/', cache=config.cache, 
    globals=t_globals)
render._keywords['globals']['render'] = render

def listing(**k):
    k['what'] = 'distinct category'
    l = db.listing(**k)
    return render.listing(l, 7)
